import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { OrderStatus, OrderSummary } from '../../../models/order.model';
import { OrderService } from '../../../services/order.service';

@Component({
  selector: 'app-admin-orders',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './admin-orders.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class AdminOrdersComponent implements OnInit {
  private readonly orders = inject(OrderService);
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
  totalPages = 0;
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
    this.loading = true;
    this.error = '';
    this.orders.findAll(this.selectedStatus, page, this.pageSize).subscribe({
      next: (response) => {
        this.items = response.content;
        this.page = response.number;
        this.totalPages = response.totalPages;
        this.totalElements = response.totalElements;
        this.loading = false;
      },
      error: () => {
        this.error = 'Không thể tải danh sách đơn hàng.';
        this.loading = false;
      },
    });
  }

  update(order: OrderSummary, status: OrderStatus): void {
    this.error = '';
    this.orders.updateStatus(order.id, status).subscribe({
      next: () => this.load(this.page),
      error: () => (this.error = 'Không thể cập nhật trạng thái đơn hàng.'),
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
}
