import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  DestroyRef,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { OrderSummary, OrderStatus } from '../../models/order.model';
import { EMPTY, Subject, catchError, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-order-history',
  imports: [CommonModule, RouterLink],
  templateUrl: './order-history.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './order-history.component.scss',
})
export class OrderHistoryComponent implements OnInit {
  private readonly orders = inject(OrderService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly requests = new Subject<number>();
  status: OrderStatus | '' = '';
  readonly labels: Record<OrderStatus, string> = {
    PENDING: 'Chờ xác nhận',
    CONFIRMED: 'Chờ vận chuyển',
    SHIPPING: 'Đang vận chuyển',
    DELIVERED: 'Đã giao · Chờ nhận hàng',
    COMPLETED: 'Hoàn thành',
    CANCELLED: 'Đã hủy',
  };
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
              this.error = 'Không thể tải lịch sử đơn hàng.';
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
