import { CommonModule } from '@angular/common';
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
} from '../../models/order.model';
import { CatalogService } from '../../services/catalog.service';
import { OrderService } from '../../services/order.service';
import { CartService } from '../../services/cart.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

@Component({
  selector: 'app-order-detail',
  imports: [CommonModule, RouterLink],
  templateUrl: './order-detail.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class OrderDetailComponent implements OnInit {
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
        error: () => {
          this.reviewStatusError =
            'Chưa tải được trạng thái đánh giá. Quyền đánh giá sẽ được kiểm tra tại trang sản phẩm.';
        },
      });
  }
  readonly statusLabels = ORDER_STATUS_LABELS;
  private readonly route = inject(ActivatedRoute);
  private readonly orders = inject(OrderService);
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  cancelling = false;
  reordering = false;
  actionError = '';

  cancel(): void {
    if (
      this.adminMode ||
      !this.order ||
      this.order.status !== 'PENDING' ||
      this.cancelling ||
      this.confirming
    ) {
      return;
    }
    if (
      !window.confirm('Hủy đơn hàng này? Tồn kho và lượt voucher sẽ được hoàn.')
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
          this.actionError =
            response.error?.message ??
            'Không hủy được đơn. Vui lòng tải lại trạng thái.';
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
            'Sản phẩm hoặc phân loại không còn khả dụng. Hãy chọn lại từ cửa hàng.';
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
        error: () => {
          this.confirmationError =
            'Không thể xác nhận nhận hàng. Vui lòng tải lại đơn và thử lại.';
          this.confirming = false;
        },
      });
  }

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isInteger(id) || id <= 0) {
      this.error = 'Mã đơn hàng không hợp lệ.';
      return;
    }
    const request = this.adminMode
      ? this.orders.findAdminOrder(id)
      : this.orders.findById(id);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (order) => {
        this.order = order;
        this.loadReviewStatuses();
      },
      error: () => (this.error = 'Không tìm thấy đơn hàng.'),
    });
  }
}
