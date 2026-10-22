import { ShopDatePipe } from '../shared/shop-date.pipe';
import { ShopMoneyPipe } from '../shared/shop-money.pipe';
import { ErrorMessageService } from '../../services/error-message.service';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { ConfirmationDialogComponent } from '../shared/confirmation-dialog.component';
import { MatDialog } from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatTableModule } from '@angular/material/table';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  DestroyRef,
} from '@angular/core';
import { ActivatedRoute, RouterLink, Router } from '@angular/router';
import {
  Order,
  OrderItem,
  ORDER_STATUS_LABELS,
  PAYMENT_STATUS_LABELS,
} from '../../models/order.model';
import { CatalogService } from '../../services/catalog.service';
import { OrderService } from '../../services/order.service';
import { CartService } from '../../services/cart.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subscription, catchError, exhaustMap, take, timer } from 'rxjs';

@Component({
  selector: 'app-order-detail',
  imports: [
    ShopDatePipe,
    ShopMoneyPipe,
    TranslatePipe,
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatTableModule,
    MatProgressBarModule,
  ],
  templateUrl: './order-detail.component.html',
  styleUrl: './order-detail.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class OrderDetailComponent implements OnInit {
  private readonly errorMessages = inject(ErrorMessageService);
  private readonly translate = inject(TranslateService);
  private readonly confirmations = inject(MatDialog);
  private confirmationOpen = false;

  private readonly catalog = inject(CatalogService);
  reviewedItems = new Set<string>();
  reviewStatusError = '';

  isReviewed(item: OrderItem): boolean {
    return this.reviewedItems.has(`${item.productId}:${item.variantId ?? 0}`);
  }

  private loadReviewStatuses(): void {
    if (this.adminMode || this.order?.status !== 'COMPLETED') {
      return;
    }
    const orderId = this.order.id;
    this.catalog
      .orderReviewStatuses(orderId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (statuses) => {
          if (this.order?.id === orderId) {
            this.reviewedItems = new Set(
              statuses
                .filter((status) => status.reviewed)
                .map(
                  (status) => `${status.productId}:${status.variantId ?? 0}`,
                ),
            );
          }
        },
        error: (response) => {
          this.reviewStatusError = this.errorMessages.message(
            response,
            'orders.reviewStatusCouldNotBeLoadedEligibilityWillBe',
          );
        },
      });
  }

  get statusLabels(): typeof ORDER_STATUS_LABELS {
    return Object.fromEntries(
      Object.keys(ORDER_STATUS_LABELS).map((status) => [
        status,
        this.translate.instant('orderStatus.' + status),
      ]),
    ) as typeof ORDER_STATUS_LABELS;
  }
  get paymentLabels(): typeof PAYMENT_STATUS_LABELS {
    return Object.fromEntries(
      Object.keys(PAYMENT_STATUS_LABELS).map((status) => [
        status,
        this.translate.instant('paymentStatus.' + status),
      ]),
    ) as typeof PAYMENT_STATUS_LABELS;
  }
  paying = false;
  checkingPayment = false;
  paymentError = '';
  private paymentPolling?: Subscription;

  get canPay(): boolean {
    return (
      !this.adminMode &&
      this.order?.paymentMethod === 'VNPAY' &&
      this.order.status === 'PENDING' &&
      this.order.paymentStatus === 'PENDING' &&
      !!this.order.paymentExpiresAt &&
      Date.parse(this.order.paymentExpiresAt) > Date.now()
    );
  }

  pay(): void {
    if (
      !this.canPay ||
      this.paying ||
      this.cancelling ||
      this.checkingPayment ||
      this.confirmationOpen ||
      !this.order
    ) {
      return;
    }
    this.paying = true;
    this.paymentError = '';
    this.orders
      .createPaymentLink(this.order.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (link) => {
          try {
            this.orders.openSandboxPayment(link.paymentUrl);
          } catch {
            this.paying = false;
            this.paymentError = this.translate.instant(
              'orders.cannotOpenVnpaySandbox',
            );
          }
        },
        error: (response) => {
          this.paying = false;
          this.paymentError = this.errorMessages.message(
            response,
            'orders.cannotCreateAPaymentLinkYouCanRetryFor',
          );
        },
      });
  }

  checkPayment(): void {
    if (
      this.adminMode ||
      !this.order ||
      this.order.paymentMethod !== 'VNPAY' ||
      this.checkingPayment ||
      this.paying
    ) {
      return;
    }
    const id = this.order.id;
    this.checkingPayment = true;
    this.paymentError = '';
    this.paymentPolling = timer(0, 3000)
      .pipe(
        take(20),
        exhaustMap(() =>
          this.orders.findById(id).pipe(
            catchError(() => {
              this.paymentError = this.translate.instant(
                'orders.cannotCheckPaymentPleaseTryAgainLater',
              );
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (order) => {
          this.order = order;
          if (order.paymentStatus !== 'PENDING') {
            this.paymentPolling?.unsubscribe();
            this.checkingPayment = false;
          }
        },
        complete: () => {
          this.checkingPayment = false;
          if (this.order?.paymentStatus === 'PENDING') {
            this.paymentError = this.translate.instant(
              'orders.ipnConfirmationHasNotArrivedIfYouAlreadyPaid',
            );
          }
        },
      });
  }

  get itemColumns(): string[] {
    return !this.adminMode && this.order?.status === 'COMPLETED'
      ? ['product', 'price', 'quantity', 'total', 'review']
      : ['product', 'price', 'quantity', 'total'];
  }

  private readonly route = inject(ActivatedRoute);
  private readonly orders = inject(OrderService);
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  cancelling = false;
  reordering = false;
  actionError = '';

  private cancelConfirmed(): void {
    if (
      this.adminMode ||
      !this.order ||
      this.order.status !== 'PENDING' ||
      this.cancelling ||
      this.confirming
    ) {
      return;
    }
    this.cancelling = true;
    this.actionError = '';
    this.orders
      .cancel(this.order.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (order) => {
          this.order = order;
          this.cancelling = false;
        },
        error: (response) => {
          this.actionError = this.errorMessages.message(
            response,
            'orders.cannotCancelTheOrderPleaseReloadItsStatus',
          );
          this.cancelling = false;
        },
      });
  }

  reorder(): void {
    if (
      this.adminMode ||
      !this.order ||
      this.reordering ||
      !['COMPLETED', 'CANCELLED'].includes(this.order.status)
    ) {
      return;
    }
    this.reordering = true;
    this.actionError = '';
    this.cart
      .addOrder(this.order)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.reordering = false;
          void this.router.navigate(['/cart']);
        },
        error: (response) => {
          this.actionError =
            response.message ??
            this.translate.instant(
              'orders.thisProductOrVariantIsNoLongerAvailablePlease',
            );
          this.reordering = false;
        },
      });
  }
  order?: Order;
  error = '';
  confirming = false;
  confirmationError = '';
  readonly adminMode = this.route.snapshot.data['adminMode'] === true;

  confirmReceipt(): void {
    if (
      this.adminMode ||
      !this.order ||
      this.confirming ||
      this.order.status !== 'DELIVERED'
    ) {
      return;
    }
    this.confirming = true;
    this.confirmationError = '';
    this.orders
      .confirmReceipt(this.order.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (order) => {
          this.order = order;
          this.confirming = false;
          this.loadReviewStatuses();
        },
        error: (response) => {
          this.confirmationError = this.errorMessages.message(
            response,
            'orders.cannotConfirmReceiptReloadTheOrderAndTryAgain',
          );
          this.confirming = false;
        },
      });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id <= 0) {
      this.error = this.translate.instant('orders.invalidOrderId');
      return;
    }
    const request = this.adminMode
      ? this.orders.findAdminOrder(id)
      : this.orders.findById(id);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (order) => {
        this.order = order;
        this.loadReviewStatuses();
        if (!this.adminMode && order.paymentMethod === 'VNPAY') {
          const action = this.route.snapshot.queryParamMap?.get('payment');
          if (action === 'start') {
            this.pay();
          } else if (
            action === 'returned' &&
            order.paymentStatus === 'PENDING'
          ) {
            this.checkPayment();
          }
        }
      },
      error: () =>
        (this.error = this.translate.instant('orders.orderNotFound')),
    });
  }

  cancel(): void {
    if (
      this.confirmationOpen ||
      this.adminMode ||
      !this.order ||
      this.order.status !== 'PENDING' ||
      this.cancelling ||
      this.confirming
    ) {
      return;
    }
    this.confirmationOpen = true;
    this.confirmations
      .open(ConfirmationDialogComponent, {
        data: this.translate.instant(
          'orders.cancelThisOrderStockAndVoucherUsageWillBe',
        ),
        width: '440px',
        maxWidth: 'calc(100vw - 32px)',
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed: boolean | undefined) => {
        this.confirmationOpen = false;
        if (confirmed === true) {
          this.cancelConfirmed();
        }
      });
  }
}
