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
  readonly cart = inject(CartService);

  lines: CartLine[] = [];
  loading = true;
  error = '';

  get subtotal(): number {
    return this.lines
      .filter((line) => this.cart.isSelected(line.product.id, line.variantId))
      .reduce((total, line) => total + line.lineTotal, 0);
  }

  get allSelected(): boolean {
    return (
      this.lines.length > 0 &&
      this.lines.every((line) =>
        this.cart.isSelected(line.product.id, line.variantId),
      )
    );
  }

  get canCheckout(): boolean {
    const selected = this.lines.filter((line) =>
      this.cart.isSelected(line.product.id, line.variantId),
    );
    return (
      selected.length > 0 &&
      selected.every((line) => (line.product.stock ?? 0) >= line.quantity)
    );
  }

  ngOnInit(): void {
    this.load();
  }

  update(line: CartLine, quantity: number): void {
    if (quantity > (line.product.stock ?? 0)) {
      this.error = 'Số lượng vượt quá tồn kho.';
      return;
    }
    this.cart.setQuantity(line.product.id, quantity, line.variantId);
    this.load();
  }

  remove(productId: number, variantId?: number): void {
    this.cart.remove(productId, variantId);
    this.load();
  }

  private load(): void {
    this.loading = true;
    this.error = '';
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
