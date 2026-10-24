import { ShopDatePipe } from '../../shared/shop-date.pipe';
import { ShopNumberPipe } from '../../shared/shop-number.pipe';
import { ShopMoneyPipe } from '../../shared/shop-money.pipe';
import { ErrorMessageService } from '../../../services/error-message.service';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import {
  MatPaginatorIntl,
  MatPaginatorModule,
  PageEvent,
} from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { createMaterialPaginatorIntl } from '../../shared/material-paginator-intl';
import { MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import {
  ORDER_STATUSES,
  PAYMENT_STATUSES,
  OrderStatus,
  OrderSummary,
  PaymentStatus,
} from '../../../models/order.model';
import { OrderService } from '../../../services/order.service';

@Component({
  selector: 'app-admin-orders',
  providers: [
    { provide: MatPaginatorIntl, useFactory: createMaterialPaginatorIntl },
  ],
  styleUrl: './admin-orders.component.scss',
  imports: [
    ShopDatePipe,
    ShopNumberPipe,
    ShopMoneyPipe,
    TranslatePipe,
    CommonModule,
    FormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatSelectModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTableModule,
    MatTooltipModule,
  ],
  templateUrl: './admin-orders.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class AdminOrdersComponent implements OnInit {
  private readonly errorMessages = inject(ErrorMessageService);
  private readonly translate = inject(TranslateService);
  private readonly orders = inject(OrderService);
  private readonly destroyRef = inject(DestroyRef);
  get labels(): Record<OrderStatus, string> {
    return Object.fromEntries(
      ORDER_STATUSES.map((status) => [
        status,
        this.translate.instant('orderStatus.' + status),
      ]),
    ) as Record<OrderStatus, string>;
  }
  get paymentLabels(): Record<PaymentStatus, string> {
    return Object.fromEntries(
      PAYMENT_STATUSES.map((status) => [
        status,
        this.translate.instant('paymentStatus.' + status),
      ]),
    ) as Record<PaymentStatus, string>;
  }
  paymentLabel(order: OrderSummary): string {
    return this.paymentLabels[order.paymentStatus ?? 'UNPAID'];
  }

  readonly columns = [
    'number',
    'customer',
    'created',
    'total',
    'status',
    'payment',
    'actions',
  ];
  readonly updating = new Set<number>();
  private loadRevision = 0;
  statusLabel(status: OrderStatus): string {
    return this.labels[status];
  }

  private readonly route = inject(ActivatedRoute);
  pageSize = 10;
  totalElements = 0;
  readonly statuses: Array<OrderStatus | ''> = [
    '',
    'PENDING',
    'CONFIRMED',
    'SHIPPING',
    'DELIVERED',
    'COMPLETED',
    'CANCELLED',
  ];
  items: OrderSummary[] = [];
  selectedStatus: OrderStatus | '' = '';
  page = 0;

  changePage(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.load(event.pageIndex);
  }

  loading = false;
  error = '';

  ngOnInit(): void {
    const status = this.route.snapshot.queryParamMap.get('status');
    if (status && this.statuses.includes(status as OrderStatus)) {
      this.selectedStatus = status as OrderStatus;
    }
    this.load();
  }

  load(page = 0): void {
    const revision = ++this.loadRevision;
    this.loading = true;
    this.error = '';
    this.orders
      .findAll(this.selectedStatus, page, this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (revision !== this.loadRevision) {
            return;
          }
          if (page > 0 && page >= response.totalPages) {
            this.load(Math.max(0, response.totalPages - 1));
            return;
          }
          this.items = response.content;
          this.page = response.number;
          this.totalElements = response.totalElements;
          this.loading = false;
        },
        error: (error) => {
          if (revision !== this.loadRevision) {
            return;
          }
          this.error = this.errorMessages.message(
            error,
            'admin.cannotLoadOrders',
          );
          this.loading = false;
        },
      });
  }

  update(order: OrderSummary, status: OrderStatus): void {
    if (
      this.updating.has(order.id) ||
      !this.availableStatuses(order).includes(status)
    ) {
      return;
    }
    this.updating.add(order.id);
    this.error = '';
    this.orders
      .updateStatus(order.id, status)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.updating.delete(order.id);
          this.load(this.page);
        },
        error: (error) => {
          this.updating.delete(order.id);
          this.error = this.errorMessages.message(
            error,
            'admin.cannotUpdateOrderStatusPleaseReloadTheList',
          );
        },
      });
  }

  nextStatuses(status: OrderStatus): OrderStatus[] {
    switch (status) {
      case 'PENDING':
        return ['CONFIRMED', 'CANCELLED'];
      case 'CONFIRMED':
        return ['SHIPPING', 'CANCELLED'];
      case 'SHIPPING':
        return ['DELIVERED'];
      default:
        return [];
    }
  }

  availableStatuses(order: OrderSummary): OrderStatus[] {
    return this.nextStatuses(order.status).filter((next) => {
      if (order.paymentMethod !== 'VNPAY') {
        return true;
      }
      if (next === 'CONFIRMED') {
        return order.paymentStatus === 'PAID';
      }
      return (
        next !== 'CANCELLED' ||
        (order.paymentStatus !== 'PAID' &&
          order.paymentStatus !== 'REVIEW_REQUIRED')
      );
    });
  }
}
