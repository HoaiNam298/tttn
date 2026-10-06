import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { OrderSummary } from '../../../models/order.model';
import { CatalogService } from '../../../services/catalog.service';
import { OrderService } from '../../../services/order.service';

@Component({
  selector: 'app-admin-dashboard',
  imports: [CommonModule, RouterLink],
  templateUrl: './admin-dashboard.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class AdminDashboardComponent implements OnInit {
  private readonly catalog = inject(CatalogService);
  private readonly orders = inject(OrderService);
  loading = false;
  error = '';
  productCount = 0;
  categoryCount = 0;
  orderCount = 0;
  pendingCount = 0;
  recentOrders: OrderSummary[] = [];

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    forkJoin({
      products: this.catalog.products('', undefined, 0, 1),
      categories: this.catalog.categoryPage('', 0, 1),
      orders: this.orders.findAll('', 0, 5),
      pending: this.orders.findAll('PENDING', 0, 1),
    }).subscribe({
      next: (data) => {
        this.productCount = data.products.totalElements;
        this.categoryCount = data.categories.totalElements;
        this.orderCount = data.orders.totalElements;
        this.pendingCount = data.pending.totalElements;
        this.recentOrders = data.orders.content;
        this.loading = false;
      },
      error: () => {
        this.error = 'Không thể tải tổng quan cửa hàng.';
        this.loading = false;
      },
    });
  }
}
