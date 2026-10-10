import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { CartLine } from '../../models/cart-line.model';
import { CartService } from '../../services/cart.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, Subject, catchError, switchMap, tap } from 'rxjs';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDividerModule } from '@angular/material/divider';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@Component({
  selector: 'app-cart',
  imports: [
    CommonModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatCheckboxModule,
    MatDividerModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './cart.component.html',
  styleUrl: './cart.component.scss',
})
export class CartComponent implements OnInit {
  readonly cart = inject(CartService);
  readonly maxQuantity = 100;
  private readonly destroyRef = inject(DestroyRef);
  private readonly refresh = new Subject<void>();
  private loadFailed = false;

  lines: CartLine[] = [];
  loading = true;
  error = '';

  get selectedCount(): number {
    return this.lines.filter((line) =>
      this.cart.isSelected(line.product.id, line.variantId),
    ).length;
  }

  get partiallySelected(): boolean {
    return this.selectedCount > 0 && !this.allSelected;
  }

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
      !this.loading &&
      !this.loadFailed &&
      selected.length > 0 &&
      selected.every(
        (line) =>
          Number.isInteger(line.quantity) &&
          line.quantity >= 1 &&
          line.quantity <= this.maxQuantity &&
          (line.product.stock ?? 0) >= line.quantity,
      )
    );
  }

  ngOnInit(): void {
    this.refresh
      .pipe(
        tap(() => {
          this.loading = true;
          this.error = '';
          this.loadFailed = false;
        }),
        switchMap(() =>
          this.cart.lines().pipe(
            catchError(() => {
              this.error =
                'Không thể tải thông tin sản phẩm trong giỏ hàng. Vui lòng thử lại trước khi đặt hàng.';
              this.loadFailed = true;
              this.loading = false;
              return EMPTY;
            }),
          ),
        ),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((lines) => {
        this.lines = lines;
        this.loading = false;
      });
    this.load();
  }

  update(line: CartLine, quantity: number): void {
    if (this.loading || !Number.isInteger(quantity) || quantity < 1) {
      return;
    }
    if (quantity > this.maxQuantity) {
      this.error = `Mỗi sản phẩm hoặc phân loại chỉ được đặt tối đa ${this.maxQuantity} sản phẩm.`;
      return;
    }
    if (quantity > (line.product.stock ?? 0)) {
      this.error = 'Số lượng vượt quá tồn kho.';
      return;
    }
    this.cart.setQuantity(line.product.id, quantity, line.variantId);
    this.load();
  }

  remove(productId: number, variantId?: number): void {
    if (this.loading) {
      return;
    }
    this.cart.remove(productId, variantId);
    this.load();
  }

  load(): void {
    this.refresh.next();
  }
}
