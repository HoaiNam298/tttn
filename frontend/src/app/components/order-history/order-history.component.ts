import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { OrderSummary } from '../../models/order.model';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-order-history',
  imports: [CommonModule, RouterLink],
  templateUrl: './order-history.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class OrderHistoryComponent implements OnInit {
  private readonly orders = inject(OrderService);
  items: OrderSummary[] = [];
  page = 0;
  totalPages = 0;
  loading = false;
  error = '';

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    this.loading = true;
    this.error = '';
    this.orders.findMine(page).subscribe({
      next: (response) => {
        this.items = response.content;
        this.page = response.number;
        this.totalPages = response.totalPages;
        this.loading = false;
      },
      error: () => {
        this.error = 'Không thể tải lịch sử đơn hàng.';
        this.loading = false;
      },
    });
  }
}
