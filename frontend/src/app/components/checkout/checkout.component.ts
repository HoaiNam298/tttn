import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  OnInit,
  inject,
  DestroyRef,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink, ActivatedRoute } from '@angular/router';
import { forkJoin, map } from 'rxjs';
import { CartLine, toCartLine } from '../../models/cart-line.model';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { OrderService } from '../../services/order.service';
import { CatalogService } from '../../services/catalog.service';
import { AddressService } from '../../services/address.service';
import { Address } from '../../models/address.model';
import { CheckoutAttempt } from '../../models/checkout-attempt.model';
import { CreateOrderPayload } from '../../dtos/create-order.dto';
import { CheckoutQuote } from '../../responses/checkout-quote.response';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-checkout',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatProgressBarModule,
    MatDividerModule,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './checkout.component.html',
  styleUrl: './checkout.component.scss',
})
export class CheckoutComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly cart = inject(CartService);
  private readonly orders = inject(OrderService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly catalog = inject(CatalogService);
  private readonly addressService = inject(AddressService);
  private readonly destroyRef = inject(DestroyRef);
  quote?: CheckoutQuote;
  quoting = false;
  quoteError = '';
  private quoteRevision = 0;
  addresses: Address[] = [];
  addressError = '';
  pending?: CheckoutAttempt;
  buyNow = false;
  private source = '';
  private readonly attemptKey = `shopapp_checkout_attempt_${this.auth.session()?.user.id}`;

  selectAddress(id: number): void {
    if (this.submitting || this.pending) {
      return;
    }
    const address = this.addresses.find((item) => item.id === id);
    if (address) {
      this.form.patchValue({
        recipientName: address.recipientName,
        phoneNumber: address.phoneNumber,
        shippingAddress: address.shippingAddress,
      });
    }
  }

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
    shippingMethod: this.fb.nonNullable.control<'STANDARD' | 'EXPRESS'>(
      'STANDARD',
    ),
    voucherCode: [
      '',
      [
        Validators.maxLength(40),
        Validators.pattern(/^$|^[A-Za-z0-9_-]{3,40}$/),
      ],
    ],
  });

  get subtotal(): number {
    return this.lines.reduce((total, line) => total + line.lineTotal, 0);
  }

  ngOnInit(): void {
    const query = this.route.snapshot.queryParamMap;
    this.buyNow = query.has('productId');
    const productId = Number(query.get('productId'));
    const quantity = Number(query.get('quantity') ?? 1);
    const variantId = query.has('variantId')
      ? Number(query.get('variantId'))
      : undefined;
    this.source = this.buyNow ? `buy:${productId}:${quantity}` : 'cart';
    if (this.buyNow && variantId != null) {
      this.source += `:${variantId}`;
    }
    if (
      this.buyNow &&
      (!Number.isInteger(productId) ||
        productId <= 0 ||
        !Number.isInteger(quantity) ||
        quantity < 1 ||
        quantity > 100 ||
        (variantId != null && (!Number.isInteger(variantId) || variantId < 1)))
    ) {
      this.error = 'Thông tin mua ngay không hợp lệ.';
      this.loading = false;
      return;
    }
    try {
      const saved = JSON.parse(
        sessionStorage.getItem(this.attemptKey) ?? 'null',
      ) as CheckoutAttempt | null;
      if (
        saved?.source === this.source &&
        saved.payload.requestId &&
        saved.payload.items.length > 0
      ) {
        this.pending = saved;
      }
    } catch {
      sessionStorage.removeItem(this.attemptKey);
    }
    const user = this.auth.session()?.user;
    if (user) {
      this.form.patchValue({
        recipientName: user.fullName,
        phoneNumber: user.phoneNumber,
        shippingAddress: user.address,
      });
    }
    if (this.pending) {
      this.form.patchValue(this.pending.payload);
    }
    this.form.valueChanges
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (!this.pending) {
          this.invalidateQuote();
        }
      });
    if (this.pending) {
      this.form.disable({ emitEvent: false });
    }
    this.addressService
      .findMine()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (addresses) => {
          this.addresses = addresses;
          const defaultAddress = addresses.find((item) => item.defaultAddress);
          if (defaultAddress && !this.pending && !this.form.dirty) {
            this.form.patchValue({
              recipientName: defaultAddress.recipientName,
              phoneNumber: defaultAddress.phoneNumber,
              shippingAddress: defaultAddress.shippingAddress,
            });
          }
        },
        error: () => {
          this.addressError =
            'Không thể tải sổ địa chỉ. Bạn vẫn có thể nhập địa chỉ trực tiếp.';
        },
      });
    const request = this.pending
      ? forkJoin(
          this.pending.payload.items.map((item) =>
            this.catalog
              .product(item.productId)
              .pipe(
                map((product) =>
                  toCartLine(product, item.quantity, item.variantId),
                ),
              ),
          ),
        )
      : this.buyNow
        ? this.catalog
            .product(productId)
            .pipe(map((product) => [toCartLine(product, quantity, variantId)]))
        : this.cart.lines(true);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (lines) => {
        this.lines = lines;
        this.loading = false;
        if (!this.pending) {
          this.refreshQuote();
        }
        if (
          !this.pending &&
          lines.some((line) => (line.product.stock ?? 0) < line.quantity)
        ) {
          this.error =
            'Sản phẩm không đủ tồn kho. Vui lòng điều chỉnh số lượng.';
        }
      },
      error: () => {
        this.error = 'Không thể tải giỏ hàng.';
        this.loading = false;
      },
    });
  }

  submit(): void {
    if (
      this.form.invalid ||
      this.loading ||
      (!this.pending && this.lines.length === 0) ||
      this.submitting
    ) {
      this.form.markAllAsTouched();
      return;
    }
    if (
      !this.pending &&
      this.lines.some((line) => (line.product.stock ?? 0) < line.quantity)
    ) {
      this.error = 'Sản phẩm không đủ tồn kho.';
      return;
    }
    if (!this.pending) {
      if (!this.quote || this.quoting) {
        this.refreshQuote();
        return;
      }
      this.pending = {
        source: this.source,
        payload: {
          ...this.quotePayload(),
          expectedTotal: this.quote.total,
          requestId: crypto.randomUUID(),
        },
      };
      sessionStorage.setItem(this.attemptKey, JSON.stringify(this.pending));
    }
    this.submitting = true;
    this.form.disable({ emitEvent: false });
    this.error = '';
    this.orders
      .create(this.pending.payload)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (order) => {
          if (!this.buyNow) {
            this.cart.removePurchased(this.pending!.payload.items);
          }
          sessionStorage.removeItem(this.attemptKey);
          void this.router.navigate(['/orders', order.id, 'success']);
        },
        error: (response) => {
          this.error =
            response.error?.message ?? 'Đặt hàng thất bại. Vui lòng thử lại.';
          this.submitting = false;
          if (
            response.status >= 400 &&
            response.status < 500 &&
            response.status !== 408
          ) {
            this.pending = undefined;
            this.form.enable({ emitEvent: false });
            this.invalidateQuote();
            sessionStorage.removeItem(this.attemptKey);
          }
        },
      });
  }

  invalidateQuote(): void {
    this.quoteRevision++;
    this.quote = undefined;
    this.quoting = false;
  }

  refreshQuote(): void {
    if (this.pending || this.lines.length === 0) {
      return;
    }
    this.invalidateQuote();
    this.quoteError = '';
    if (this.form.invalid) {
      this.quoteError =
        'Nhập thông tin nhận hàng và mã voucher hợp lệ để tính tổng tiền.';
      return;
    }
    const revision = this.quoteRevision;
    this.quoting = true;
    this.orders
      .quote(this.quotePayload())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (quote) => {
          if (revision === this.quoteRevision) {
            this.quote = quote;
            this.quoting = false;
          }
        },
        error: (response) => {
          if (revision === this.quoteRevision) {
            this.quoteError =
              response.error?.message ??
              'Không tính được tổng tiền. Hãy kiểm tra voucher và thử lại.';
            this.quoting = false;
          }
        },
      });
  }

  private quotePayload(): CreateOrderPayload {
    const value = this.form.getRawValue();
    return {
      ...value,
      recipientName: value.recipientName.trim(),
      shippingAddress: value.shippingAddress.trim(),
      voucherCode: value.voucherCode.trim().toUpperCase(),
      items: this.lines.map((line) => ({
        productId: line.product.id,
        quantity: line.quantity,
        ...(line.variantId != null ? { variantId: line.variantId } : {}),
      })),
    };
  }
}
