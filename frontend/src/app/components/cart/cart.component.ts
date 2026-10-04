import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartLine } from '../../models/cart-line.model';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-cart',
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './cart.component.html',
})
export class CartComponent implements OnInit {
  private readonly cart = inject(CartService);

  lines: CartLine[] = [];
  loading = true;
  error = '';

  get subtotal(): number {
    return this.lines.reduce((total, line) => total + line.lineTotal, 0);
  }

  ngOnInit(): void {
    this.load();
  }

  update(line: CartLine, quantity: number): void {
    this.cart.setQuantity(line.product.id, quantity);
    this.load();
  }

  remove(productId: number): void {
    this.cart.remove(productId);
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.cart.lines().subscribe({
      next: (lines) => {
        this.lines = lines;
        this.loading = false;
      },
      error: () => {
        this.error = 'Không thể tải thông tin sản phẩm trong giỏ hàng.';
        this.loading = false;
      },
    });
  }
}
