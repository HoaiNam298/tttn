import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Subject, of } from 'rxjs';
import { CartService } from '../../services/cart.service';
import { CartLine } from '../../models/cart-line.model';
import { CartComponent } from './cart.component';

describe('Material cart and checkout availability', () => {
  let selected: Set<number>;
  let lines: CartLine[];
  const cart = {
    lines: vi.fn(),
    setQuantity: vi.fn(),
    remove: vi.fn(),
    isSelected: vi.fn(),
    select: vi.fn(),
    selectAll: vi.fn(),
  };

  beforeEach(() => {
    vi.clearAllMocks();
    selected = new Set([10]);
    lines = [10, 20].map((variantId) => ({
      variantId,
      variantName: `SKU ${variantId}`,
      quantity: 2,
      lineTotal: 200000,
      product: {
        id: 1,
        name: 'Test phone',
        price: 100000,
        stock: 5,
        thumbnail: null,
        description: '',
        category: { id: 1, name: 'Phones' },
        createdAt: '',
        updatedAt: '',
      },
    }));
    cart.lines.mockReturnValue(of(lines));
    cart.isSelected.mockImplementation((_id: number, variantId: number) =>
      selected.has(variantId),
    );
    TestBed.configureTestingModule({
      imports: [CartComponent],
      providers: [provideRouter([]), { provide: CartService, useValue: cart }],
    });
  });

  it('calculates the subtotal using selected SKUs only', () => {
    const fixture = TestBed.createComponent(CartComponent);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    expect(component.subtotal).toBe(200000);
    expect(component.selectedCount).toBe(1);
    expect(component.partiallySelected).toBe(true);
    expect(component.canCheckout).toBe(true);
    expect(
      fixture.nativeElement.querySelector('a[href="/checkout"]'),
    ).not.toBeNull();
  });

  it('does not allow checkout with no selection or insufficient SKU stock', () => {
    const component = TestBed.createComponent(CartComponent).componentInstance;
    component.ngOnInit();
    selected.clear();
    expect(component.canCheckout).toBe(false);
    selected.add(10);
    lines[0].product.stock = 1;
    expect(component.canCheckout).toBe(false);
  });

  it('guards quantity boundaries and keeps variant mapping when updating', () => {
    const component = TestBed.createComponent(CartComponent).componentInstance;
    component.ngOnInit();
    component.update(lines[0], 0);
    component.update(lines[0], 1.5);
    component.update(lines[0], 6);
    expect(cart.setQuantity).not.toHaveBeenCalled();
    component.update(lines[0], 3);
    expect(cart.setQuantity).toHaveBeenCalledWith(1, 3, 10);
  });

  it('blocks mutations and checkout while refreshed stock is pending', () => {
    const pending = new Subject<CartLine[]>();
    const component = TestBed.createComponent(CartComponent).componentInstance;
    component.ngOnInit();
    cart.lines.mockReturnValue(pending);
    component.load();
    component.update(lines[0], 3);
    component.remove(1, 10);
    expect(cart.setQuantity).not.toHaveBeenCalled();
    expect(cart.remove).not.toHaveBeenCalled();
    expect(component.canCheckout).toBe(false);
    pending.next(lines);
    expect(component.canCheckout).toBe(true);
  });

  it('enforces the API limit of 100 units even when stock is higher', () => {
    const component = TestBed.createComponent(CartComponent).componentInstance;
    component.ngOnInit();
    lines[0].product.stock = 1000;
    component.update(lines[0], 101);
    expect(cart.setQuantity).not.toHaveBeenCalled();
    lines[0].quantity = 101;
    expect(component.canCheckout).toBe(false);
    lines[0].quantity = 100;
    expect(component.canCheckout).toBe(true);
  });

  it('keeps checkout blocked after refresh failure and allows recovery', () => {
    const fixture = TestBed.createComponent(CartComponent);
    fixture.detectChanges();
    const pending = new Subject<CartLine[]>();
    cart.lines.mockReturnValue(pending);
    fixture.componentInstance.load();
    pending.error(new Error('Network unavailable'));
    fixture.detectChanges();
    expect(fixture.componentInstance.lines).toHaveLength(2);
    expect(fixture.componentInstance.canCheckout).toBe(false);
    expect(
      fixture.nativeElement.querySelector('a[href="/checkout"]'),
    ).toBeNull();
    expect(
      fixture.nativeElement.querySelector('[role="alert"]'),
    ).not.toBeNull();
    cart.lines.mockReturnValue(of(lines));
    fixture.componentInstance.load();
    expect(fixture.componentInstance.canCheckout).toBe(true);
    expect(fixture.componentInstance.error).toBe('');
  });

  it('ignores an older response after retry starts a newer request', () => {
    const oldRequest = new Subject<CartLine[]>();
    const newRequest = new Subject<CartLine[]>();
    cart.lines.mockReturnValueOnce(oldRequest).mockReturnValueOnce(newRequest);
    const component = TestBed.createComponent(CartComponent).componentInstance;
    component.ngOnInit();
    component.load();
    newRequest.next([lines[1]]);
    oldRequest.next([lines[0]]);
    expect(component.lines[0].variantId).toBe(20);
  });

  it('removes only the requested SKU and displays the empty state', () => {
    const fixture = TestBed.createComponent(CartComponent);
    fixture.detectChanges();
    cart.lines.mockReturnValue(of([]));
    fixture.componentInstance.remove(1, 20);
    fixture.detectChanges();
    expect(cart.remove).toHaveBeenCalledWith(1, 20);
    expect(fixture.nativeElement.textContent).toContain(
      'Giỏ hàng của bạn đang trống',
    );
  });

  it('unsubscribes pending loads when the page is destroyed', () => {
    const pending = new Subject<CartLine[]>();
    cart.lines.mockReturnValue(pending);
    const fixture = TestBed.createComponent(CartComponent);
    fixture.detectChanges();
    fixture.destroy();
    pending.next(lines);
    expect(fixture.componentInstance.lines).toHaveLength(0);
  });
});
