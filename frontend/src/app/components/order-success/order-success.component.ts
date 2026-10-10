import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Order } from '../../models/order.model';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-order-success',
  imports: [CommonModule, RouterLink, MatButtonModule, MatCardModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './order-success.component.html',
})
export class OrderSuccessComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly orders = inject(OrderService);

  order?: Order;
  error = '';

  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.orders.findById(id).subscribe({
      next: (order) => (this.order = order),
      error: () => (this.error = 'Không thể tải thông tin đơn hàng.'),
    });
  }
}
