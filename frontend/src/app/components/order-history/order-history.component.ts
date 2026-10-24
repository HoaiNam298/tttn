import { ShopDatePipe } from '../shared/shop-date.pipe';
import { ShopMoneyPipe } from '../shared/shop-money.pipe';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import {
  MatPaginatorIntl,
  MatPaginatorModule,
} from '@angular/material/paginator';
import { createMaterialPaginatorIntl } from '../shared/material-paginator-intl';

import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  DestroyRef,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import {
  OrderSummary,
  OrderStatus,
  PAYMENT_STATUSES,
  PaymentStatus,
} from '../../models/order.model';
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { OrderService } from '../../services/order.service';

@Component({
  providers: [
    { provide: MatPaginatorIntl, useFactory: createMaterialPaginatorIntl },
  ],
  selector: 'app-order-history',
  imports: [
    ShopDatePipe,
    ShopMoneyPipe,
    TranslatePipe,
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatProgressBarModule,
    MatPaginatorModule,
  ],
  templateUrl: './order-history.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './order-history.component.scss',
})
export class OrderHistoryComponent implements OnInit {
  private readonly translate = inject(TranslateService);
  get paymentLabels(): Record<PaymentStatus, string> {
    return Object.fromEntries(
      PAYMENT_STATUSES.map((status) => [
        status,
        this.translate.instant('paymentStatus.' + status),
      ]),
    ) as Record<PaymentStatus, string>;
  }
  private readonly orders = inject(OrderService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly requests = new Subject<number>();
  status: OrderStatus | '' = '';
  get labels(): Record<OrderStatus, string> {
    return {
      PENDING: this.translate.instant('orders.awaitingConfirmation'),
      CONFIRMED: this.translate.instant('orders.awaitingShipment'),
      SHIPPING: this.translate.instant('orders.shipping3'),
      DELIVERED: this.translate.instant('orders.deliveredAwaitingReceipt'),
      COMPLETED: this.translate.instant('orders.completed'),
      CANCELLED: this.translate.instant('orders.cancelled'),
    };
  }
  readonly tabs: (OrderStatus | '')[] = [
    '',
    'PENDING',
    'CONFIRMED',
    'SHIPPING',
    'DELIVERED',
    'COMPLETED',
    'CANCELLED',
  ];
  items: OrderSummary[] = [];
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = false;
  error = '';

  ngOnInit(): void {
    this.requests
      .pipe(
        switchMap((page) => {
          this.loading = true;
          this.error = '';
          return this.orders.findMine(page, 10, this.status).pipe(
            catchError(() => {
              this.error = this.translate.instant(
                'orders.cannotLoadOrderHistory',
              );
              this.loading = false;
              this.totalPages = 0;
              return EMPTY;
            }),
          );
        }),
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((response) => {
        this.items = response.content;
        this.page = response.number;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.loading = false;
      });
    this.load();
  }

  load(page = 0): void {
    this.requests.next(page);
  }

  selectStatus(status: OrderStatus | ''): void {
    this.status = status;
    this.load();
  }
}
