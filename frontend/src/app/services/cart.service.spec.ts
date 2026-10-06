import { TestBed } from '@angular/core/testing';
import { of, firstValueFrom } from 'rxjs';
import { Product } from '../models/product.model';
import { CartService } from './cart.service';
import { CatalogService } from './catalog.service';
import { AuthService } from './auth.service';
import { signal } from '@angular/core';
import { Order } from '../models/order.model';

describe('CartService', () => {
  let service: CartService;
  const userId = signal<number | null>(null);

  beforeEach(() => {
    localStorage.removeItem('shopapp_cart');
    localStorage.removeItem('shopapp_cart_user_101');
    localStorage.removeItem('shopapp_cart_user_102');
    userId.set(null);
    TestBed.configureTestingModule({
      providers: [
        CartService,
        { provide: AuthService, useValue: { userId } },
        {
          provide: CatalogService,
          useValue: {
            product: (id: number) => of(product(id)),
          },
        },
      ],
    });
    service = TestBed.inject(CartService);
  });

  afterEach(() => {
    for (const key of [
      'shopapp_cart',
      'shopapp_cart_user_101',
      'shopapp_cart_user_102',
    ]) {
      localStorage.removeItem(key);
    }
  });

  it('checks out only selected items and keeps unpurchased items', () => {
    service.add(1, 2);
    service.add(2, 3);
    service.select(2, false);
    expect(service.items(true)).toEqual([{ productId: 1, quantity: 2 }]);
    service.removePurchased(service.items(true));
    expect(service.items()).toEqual([{ productId: 2, quantity: 3 }]);
  });

  it('isolates carts between accounts and adopts the guest cart once', () => {
    service.add(1);
    userId.set(101);
    TestBed.tick();
    expect(service.items()).toEqual([{ productId: 1, quantity: 1 }]);
    userId.set(102);
    TestBed.tick();
    expect(service.items()).toEqual([]);
    service.add(2);
    userId.set(101);
    TestBed.tick();
    expect(service.items()).toEqual([{ productId: 1, quantity: 1 }]);
  });

  it('adds the same product by increasing its quantity', () => {
    service.add(1);
    service.add(1, 2);

    expect(service.count()).toBe(3);
    expect(service.items()).toEqual([{ productId: 1, quantity: 3 }]);
  });

  it('keeps different SKUs separate when selecting, updating and removing purchased lines', () => {
    service.add(8, 1, 11);
    service.add(8, 2, 12);
    service.setQuantity(8, 3, 12);
    service.select(8, false, 11);
    expect(service.items(true)).toEqual([
      { productId: 8, quantity: 3, variantId: 12 },
    ]);
    service.removePurchased([{ productId: 8, quantity: 2, variantId: 12 }]);
    expect(service.items()).toEqual([
      { productId: 8, quantity: 1, variantId: 11 },
      { productId: 8, quantity: 1, variantId: 12 },
    ]);
    service.remove(8, 11);
    expect(service.items()).toEqual([
      { productId: 8, quantity: 1, variantId: 12 },
    ]);
  });

  it('calculates cart lines from current catalog prices', () => {
    service.add(1, 2);

    service.lines().subscribe((lines) => {
      expect(lines[0].lineTotal).toBe(200000);
    });
  });

  it('reorders only available current SKUs without carrying old prices or vouchers', async () => {
    const catalog = TestBed.inject(CatalogService);
    vi.spyOn(catalog, 'product').mockReturnValue(
      of({
        ...product(8),
        stock: 5,
        variants: [
          {
            id: 41,
            sku: 'BLACK',
            name: 'Black',
            color: '',
            size: '',
            capacity: '',
            price: 150000,
            stock: 5,
            active: true,
            imageUrl: null,
          },
        ],
      }),
    );
    await firstValueFrom(
      service.addOrder({
        items: [
          {
            productId: 8,
            variantId: 41,
            productName: 'Phone',
            unitPrice: 100000,
            quantity: 2,
            lineTotal: 200000,
          },
        ],
      } as Order),
    );
    expect(service.items()).toEqual([
      { productId: 8, variantId: 41, quantity: 2 },
    ]);
    const lines = await firstValueFrom(service.lines());
    expect(lines[0].lineTotal).toBe(300000);
  });

  it('keeps the whole cart unchanged when a later reorder item is unavailable', async () => {
    service.add(1, 1);
    const catalog = TestBed.inject(CatalogService);
    vi.spyOn(catalog, 'product').mockImplementation((id) =>
      of({ ...product(id), stock: id === 8 ? 10 : 0 }),
    );
    const previous = service.items();
    await expect(
      firstValueFrom(
        service.addOrder({
          items: [
            {
              productId: 8,
              productName: 'Phone',
              unitPrice: 100000,
              quantity: 2,
              lineTotal: 200000,
            },
            {
              productId: 9,
              productName: 'Phone',
              unitPrice: 100000,
              quantity: 1,
              lineTotal: 100000,
            },
          ],
        } as Order),
      ),
    ).rejects.toThrow('Giỏ hàng chưa thay đổi');
    expect(service.items()).toEqual(previous);
  });

  function product(id: number): Product {
    return {
      id,
      name: 'Phone',
      price: 100000,
      thumbnail: '',
      description: '',
      category: { id: 1, name: 'Phone' },
      createdAt: '',
      updatedAt: '',
    };
  }
});
