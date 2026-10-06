import { Injectable, computed, inject, signal, effect } from '@angular/core';
import { Observable, forkJoin, map, of, tap } from 'rxjs';
import { CartLine, toCartLine } from '../models/cart-line.model';
import { CatalogService } from './catalog.service';
import { AuthService } from './auth.service';
import { OrderItemPayload } from '../dtos/create-order.dto';
import { Order } from '../models/order.model';

interface StoredCartItem {
  variantId?: number;
  productId: number;
  quantity: number;
  selected?: boolean;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly auth = inject(AuthService);
  private readonly revision = signal(0);
  private readonly storageKey = computed(() =>
    this.auth.userId() == null
      ? 'shopapp_cart'
      : `shopapp_cart_user_${this.auth.userId()}`,
  );
  private readonly catalog = inject(CatalogService);
  private readonly state = computed(() => {
    this.revision();
    const own = this.read(this.storageKey());
    if (this.auth.userId() == null) {
      return own;
    }
    const merged = own.map((item) => ({ ...item }));
    for (const guest of this.read('shopapp_cart')) {
      const existing = merged.find(
        (item) =>
          item.productId === guest.productId &&
          item.variantId === guest.variantId,
      );
      if (existing) {
        existing.quantity += guest.quantity;
      } else {
        merged.push({ ...guest });
      }
    }
    return merged;
  });

  readonly count = computed(() =>
    this.state().reduce((total, item) => total + item.quantity, 0),
  );

  constructor() {
    effect(() => {
      if (this.auth.userId() != null && this.read('shopapp_cart').length > 0) {
        this.save(this.state());
      }
    });
  }

  add(productId: number, quantity = 1, variantId?: number): void {
    const items = this.state().map((item) => ({ ...item }));
    const existing = items.find(
      (item) => item.productId === productId && item.variantId === variantId,
    );
    if (existing) {
      existing.quantity += quantity;
    } else {
      items.push({
        productId,
        quantity,
        selected: true,
        ...(variantId != null ? { variantId } : {}),
      });
    }
    this.save(items);
  }

  setQuantity(productId: number, quantity: number, variantId?: number): void {
    if (quantity < 1) {
      this.remove(productId, variantId);
      return;
    }
    this.save(
      this.state().map((item) =>
        item.productId === productId && item.variantId === variantId
          ? { ...item, quantity }
          : item,
      ),
    );
  }

  remove(productId: number, variantId?: number): void {
    this.save(
      this.state().filter(
        (item) =>
          !(item.productId === productId && item.variantId === variantId),
      ),
    );
  }

  clear(): void {
    this.save([]);
  }

  addOrder(order: Order): Observable<void> {
    const accountKey = this.storageKey();
    const productIds = [...new Set(order.items.map((item) => item.productId))];
    return forkJoin(productIds.map((id) => this.catalog.product(id))).pipe(
      tap((products) => {
        if (accountKey !== this.storageKey()) {
          throw new Error(
            'Tài khoản đã thay đổi. Vui lòng mua lại từ tài khoản hiện tại.',
          );
        }
        const items = this.state().map((item) => ({ ...item }));
        for (const item of order.items) {
          const product = products.find((value) => value.id === item.productId);
          if (!product) {
            throw new Error('Sản phẩm không còn khả dụng.');
          }
          const line = toCartLine(
            product,
            item.quantity,
            item.variantId ?? undefined,
          );
          const existing = items.find(
            (value) =>
              value.productId === item.productId &&
              value.variantId === (item.variantId ?? undefined),
          );
          const quantity = (existing?.quantity ?? 0) + item.quantity;
          if (quantity > (line.product.stock ?? 0) || quantity > 100) {
            throw new Error(
              'Sản phẩm hoặc SKU không đủ tồn kho để mua lại. Giỏ hàng chưa thay đổi.',
            );
          }
          if (existing) {
            existing.quantity = quantity;
            existing.selected = true;
          } else {
            items.push({
              productId: item.productId,
              quantity,
              selected: true,
              ...(item.variantId != null ? { variantId: item.variantId } : {}),
            });
          }
        }
        this.save(items);
      }),
      map(() => undefined),
    );
  }

  items(selectedOnly = false): OrderItemPayload[] {
    return this.state()
      .filter((item) => !selectedOnly || item.selected !== false)
      .map((item) => ({
        productId: item.productId,
        quantity: item.quantity,
        ...(item.variantId != null ? { variantId: item.variantId } : {}),
      }));
  }

  isSelected(productId: number, variantId?: number): boolean {
    return (
      this.state().find(
        (item) => item.productId === productId && item.variantId === variantId,
      )?.selected !== false
    );
  }

  select(productId: number, selected: boolean, variantId?: number): void {
    this.save(
      this.state().map((item) =>
        item.productId === productId && item.variantId === variantId
          ? { ...item, selected }
          : item,
      ),
    );
  }

  selectAll(selected: boolean): void {
    this.save(this.state().map((item) => ({ ...item, selected })));
  }

  removePurchased(purchased: OrderItemPayload[]): void {
    const quantities = new Map(
      purchased.map((item) => [
        `${item.productId}:${item.variantId ?? ''}`,
        item.quantity,
      ]),
    );
    this.save(
      this.state()
        .map((item) => ({
          ...item,
          quantity:
            item.quantity -
            (quantities.get(`${item.productId}:${item.variantId ?? ''}`) ?? 0),
        }))
        .filter((item) => item.quantity > 0),
    );
  }

  lines(selectedOnly = false): Observable<CartLine[]> {
    const items = this.items(selectedOnly);
    if (items.length === 0) {
      return of([]);
    }
    return forkJoin(
      items.map((item) =>
        this.catalog
          .product(item.productId)
          .pipe(
            map((product) =>
              toCartLine(product, item.quantity, item.variantId),
            ),
          ),
      ),
    );
  }

  private read(key: string): StoredCartItem[] {
    const value = localStorage.getItem(key);
    if (!value) {
      return [];
    }
    try {
      const items = JSON.parse(value) as StoredCartItem[];
      return items.filter(
        (item) =>
          Number.isInteger(item.productId) &&
          item.productId > 0 &&
          Number.isInteger(item.quantity) &&
          item.quantity > 0 &&
          (item.variantId == null ||
            (Number.isInteger(item.variantId) && item.variantId > 0)),
      );
    } catch {
      localStorage.removeItem(key);
      return [];
    }
  }

  private save(items: StoredCartItem[]): void {
    localStorage.setItem(this.storageKey(), JSON.stringify(items));
    if (this.auth.userId() != null) {
      localStorage.removeItem('shopapp_cart');
    }
    this.revision.update((value) => value + 1);
  }
}
