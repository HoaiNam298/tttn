import { Injectable, computed, inject, signal } from '@angular/core';
import { Observable, forkJoin, map, of } from 'rxjs';
import { CartLine } from '../models/cart-line.model';
import { CatalogService } from './catalog.service';

interface StoredCartItem {
  productId: number;
  quantity: number;
}

@Injectable({ providedIn: 'root' })
export class CartService {
  private readonly storageKey = 'shopapp_cart';
  private readonly catalog = inject(CatalogService);
  private readonly state = signal<StoredCartItem[]>(this.read());

  readonly count = computed(() =>
    this.state().reduce((total, item) => total + item.quantity, 0),
  );

  add(productId: number, quantity = 1): void {
    const items = [...this.state()];
    const existing = items.find((item) => item.productId === productId);
    if (existing) {
      existing.quantity += quantity;
    } else {
      items.push({ productId, quantity });
    }
    this.save(items);
  }

  setQuantity(productId: number, quantity: number): void {
    if (quantity < 1) {
      this.remove(productId);
      return;
    }
    this.save(
      this.state().map((item) =>
        item.productId === productId ? { ...item, quantity } : item,
      ),
    );
  }

  remove(productId: number): void {
    this.save(this.state().filter((item) => item.productId !== productId));
  }

  clear(): void {
    this.save([]);
  }

  items(): StoredCartItem[] {
    return [...this.state()];
  }

  lines(): Observable<CartLine[]> {
    const items = this.state();
    if (items.length === 0) {
      return of([]);
    }
    return forkJoin(
      items.map((item) =>
        this.catalog.product(item.productId).pipe(
          map((product) => ({
            product,
            quantity: item.quantity,
            lineTotal: product.price * item.quantity,
          })),
        ),
      ),
    );
  }

  private read(): StoredCartItem[] {
    const value = localStorage.getItem(this.storageKey);
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
          item.quantity > 0,
      );
    } catch {
      localStorage.removeItem(this.storageKey);
      return [];
    }
  }

  private save(items: StoredCartItem[]): void {
    this.state.set(items);
    localStorage.setItem(this.storageKey, JSON.stringify(items));
  }
}
