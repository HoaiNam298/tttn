import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CartLine } from '../../models/cart-line.model';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';

@Component({
  selector: 'app-checkout',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './checkout.component.html',
})
export class CheckoutComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly cart = inject(CartService);
  private readonly orders = inject(OrderService);
  private readonly router = inject(Router);

  readonly shippingFee = 30000;
  lines: CartLine[] = [];
  loading = true;
  submitting = false;
  error = '';
  form = this.fb.nonNullable.group({
    recipientName: ['', [Validators.required, Validators.maxLength(100)]],
    phoneNumber: [
      '',
      [Validators.required, Validators.pattern(/^[0-9+]{9,15}$/)],
    ],
    shippingAddress: ['', [Validators.required, Validators.maxLength(255)]],
    note: ['', Validators.maxLength(500)],
  });

  get subtotal(): number {
    return this.lines.reduce((total, line) => total + line.lineTotal, 0);
  }

  ngOnInit(): void {
    const user = this.auth.session()?.user;
    if (user) {
      this.form.patchValue({
        recipientName: user.fullName,
        phoneNumber: user.phoneNumber,
        shippingAddress: user.address,
      });
    }
    this.cart.lines().subscribe({
      next: (lines) => {
        this.lines = lines;
        this.loading = false;
      },
      error: () => {
        this.error = 'Không thể tải giỏ hàng.';
        this.loading = false;
      },
    });
  }

  submit(): void {
    if (this.form.invalid || this.lines.length === 0 || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting = true;
    this.error = '';
    this.orders
      .create({
        ...this.form.getRawValue(),
        items: this.cart.items(),
      })
      .subscribe({
        next: (order) => {
          this.cart.clear();
          void this.router.navigate(['/orders', order.id, 'success']);
        },
        error: (response) => {
          this.error =
            response.error?.message ?? 'Đặt hàng thất bại. Vui lòng thử lại.';
          this.submitting = false;
        },
      });
  }
}
