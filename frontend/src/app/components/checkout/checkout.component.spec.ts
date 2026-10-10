import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { of, throwError, Subject } from 'rxjs';
import { CheckoutQuote } from '../../responses/checkout-quote.response';
import { AddressService } from '../../services/address.service';
import { AuthService } from '../../services/auth.service';
import { CartService } from '../../services/cart.service';
import { CatalogService } from '../../services/catalog.service';
import { OrderService } from '../../services/order.service';
import { CheckoutComponent } from './checkout.component';

describe('Checkout request recovery', () => {
  const key = 'shopapp_checkout_attempt_901';
  const cart = { lines: vi.fn(), removePurchased: vi.fn() };
  const orders = { create: vi.fn(), quote: vi.fn() };
  const catalog = { product: vi.fn() };
  const router = { navigate: vi.fn() };

  beforeEach(() => {
    sessionStorage.removeItem(key);
    vi.resetAllMocks();
    orders.quote.mockReturnValue(
      of({
        subtotal: 200,
        shippingMethod: 'STANDARD',
        shippingFee: 30000,
        voucherCode: null,
        discount: 0,
        total: 30200,
      }),
    );
    catalog.product.mockReturnValue(
      of({ id: 8, name: 'Phone', price: 100, stock: 20 }),
    );
    TestBed.configureTestingModule({
      providers: [
        {
          provide: AuthService,
          useValue: {
            session: () => ({
              user: {
                id: 901,
                fullName: 'Customer',
                phoneNumber: '0900000000',
                address: 'HCM',
              },
            }),
          },
        },
        { provide: CartService, useValue: cart },
        { provide: OrderService, useValue: orders },
        { provide: CatalogService, useValue: catalog },
        { provide: AddressService, useValue: { findMine: () => of([]) } },
        { provide: Router, useValue: router },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({
                productId: '8',
                quantity: '2',
              }),
            },
          },
        },
      ],
    });
  });

  afterEach(() => sessionStorage.removeItem(key));

  it('uses the selected SKU price and sends its ID for buy-now', () => {
    TestBed.overrideProvider(ActivatedRoute, {
      useValue: {
        snapshot: {
          queryParamMap: convertToParamMap({
            productId: '8',
            quantity: '2',
            variantId: '41',
          }),
        },
      },
    });
    catalog.product.mockReturnValue(
      of({
        id: 8,
        name: 'Phone',
        price: 100,
        stock: 20,
        variants: [
          { id: 41, name: 'Black / 128GB', price: 250, stock: 3, active: true },
        ],
      }),
    );
    orders.create.mockReturnValue(of({ id: 32 }));
    const component = TestBed.runInInjectionContext(
      () => new CheckoutComponent(),
    );
    component.ngOnInit();
    expect(component.lines[0].lineTotal).toBe(500);
    component.submit();
    expect(orders.create.mock.calls[0][0].items).toEqual([
      { productId: 8, quantity: 2, variantId: 41 },
    ]);
    expect(cart.removePurchased).not.toHaveBeenCalled();
  });

  it('retries the same payload after a lost response without modifying the cart for buy-now', () => {
    orders.create
      .mockReturnValueOnce(throwError(() => ({ status: 0 })))
      .mockReturnValueOnce(of({ id: 31 }));
    const component = TestBed.runInInjectionContext(
      () => new CheckoutComponent(),
    );
    component.ngOnInit();
    component.submit();
    const first = orders.create.mock.calls[0][0];
    expect(first.items).toEqual([{ productId: 8, quantity: 2 }]);
    expect(first.requestId).toBeTruthy();
    expect(component.pending).toBeDefined();
    component.submit();
    expect(orders.create.mock.calls[1][0]).toEqual(first);
    expect(cart.lines).not.toHaveBeenCalled();
    expect(cart.removePurchased).not.toHaveBeenCalled();
    expect(sessionStorage.getItem(key)).toBeNull();
    expect(router.navigate).toHaveBeenCalledWith(['/orders', 31, 'success']);
  });

  it('recovers a pending request even if the catalog no longer loads its product', () => {
    const payload = {
      recipientName: 'Customer',
      phoneNumber: '0900000000',
      shippingAddress: 'HCM',
      note: '',
      requestId: crypto.randomUUID(),
      items: [{ productId: 8, quantity: 2 }],
    };
    sessionStorage.setItem(key, JSON.stringify({ source: 'buy:8:2', payload }));
    catalog.product.mockReturnValue(throwError(() => ({ status: 404 })));
    orders.create.mockReturnValue(of({ id: 31 }));
    const component = TestBed.runInInjectionContext(
      () => new CheckoutComponent(),
    );
    component.ngOnInit();
    expect(component.lines).toEqual([]);
    component.submit();
    expect(orders.create).toHaveBeenCalledWith(payload);
    expect(router.navigate).toHaveBeenCalled();
  });

  it('uses server totals and selected shipping/voucher, freezing them for retry', () => {
    orders.quote.mockReturnValue(
      of({
        subtotal: 200,
        shippingMethod: 'EXPRESS',
        shippingFee: 50000,
        voucherCode: 'SAVE10',
        discount: 50,
        total: 50150,
      }),
    );
    orders.create.mockReturnValue(throwError(() => ({ status: 0 })));
    const component = TestBed.runInInjectionContext(
      () => new CheckoutComponent(),
    );
    component.ngOnInit();
    component.form.patchValue({
      shippingMethod: 'EXPRESS',
      voucherCode: 'SAVE10',
    });
    component.refreshQuote();
    expect(component.quote?.total).toBe(50150);
    component.submit();
    const first = orders.create.mock.calls[0][0];
    expect(first.shippingMethod).toBe('EXPRESS');
    expect(first.voucherCode).toBe('SAVE10');
    expect(first.expectedTotal).toBe(50150);
    component.form.patchValue({
      shippingMethod: 'STANDARD',
      voucherCode: 'OTHER',
    });
    component.submit();
    expect(orders.create.mock.calls[1][0]).toEqual(first);
  });

  it('does not place an order when the voucher quote is rejected', () => {
    orders.quote.mockReturnValue(
      throwError(() => ({
        status: 409,
        error: { message: 'Voucher unavailable' },
      })),
    );
    const component = TestBed.runInInjectionContext(
      () => new CheckoutComponent(),
    );
    component.ngOnInit();
    component.submit();
    expect(component.quote).toBeUndefined();
    expect(component.quoteError).toBe('Voucher unavailable');
    expect(orders.create).not.toHaveBeenCalled();
  });

  it('clears a rejected quote so changed totals need confirmation again', () => {
    orders.create.mockReturnValue(
      throwError(() => ({
        status: 409,
        error: { message: 'Order total changed' },
      })),
    );
    const component = TestBed.runInInjectionContext(
      () => new CheckoutComponent(),
    );
    component.ngOnInit();
    component.submit();
    expect(component.pending).toBeUndefined();
    expect(component.quote).toBeUndefined();
    expect(component.error).toBe('Order total changed');
    expect(sessionStorage.getItem(key)).toBeNull();
  });

  it('invalidates an old quote when recipient details change', () => {
    const component = TestBed.runInInjectionContext(
      () => new CheckoutComponent(),
    );
    component.ngOnInit();
    expect(component.quote).toBeDefined();
    component.form.controls.shippingAddress.setValue('New delivery address');
    expect(component.quote).toBeUndefined();
    component.refreshQuote();
    expect(component.quote).toBeDefined();
  });

  it('ignores a quote response for details that have since changed', () => {
    const response = new Subject<CheckoutQuote>();
    orders.quote.mockReturnValue(response);
    const component = TestBed.runInInjectionContext(
      () => new CheckoutComponent(),
    );
    component.ngOnInit();
    component.form.controls.shippingAddress.setValue('Changed address');
    response.next({
      subtotal: 200,
      shippingMethod: 'STANDARD',
      shippingFee: 30000,
      voucherCode: null,
      discount: 0,
      total: 30200,
    });
    expect(component.quote).toBeUndefined();
    expect(component.quoting).toBe(false);
  });

  it('blocks duplicate submissions and freezes the pending request', () => {
    orders.create.mockReturnValue(new Subject());
    const component = TestBed.runInInjectionContext(
      () => new CheckoutComponent(),
    );
    component.ngOnInit();
    component.submit();
    component.submit();
    expect(orders.create).toHaveBeenCalledTimes(1);
    expect(component.form.disabled).toBe(true);
    expect(component.submitting).toBe(true);
  });
});
