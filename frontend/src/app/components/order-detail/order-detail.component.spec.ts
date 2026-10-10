import { TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter,
} from '@angular/router';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { Order } from '../../models/order.model';
import { OrderService } from '../../services/order.service';
import { CatalogService } from '../../services/catalog.service';
import { CartService } from '../../services/cart.service';
import { OrderDetailComponent } from './order-detail.component';

describe('Material order detail', () => {
  const order: Order = {
    id: 1,
    orderNumber: 'ORD-1',
    recipientName: 'Khách hàng',
    phoneNumber: '0901234567',
    shippingAddress: 'Hồ Chí Minh',
    note: '',
    status: 'DELIVERED',
    paymentMethod: 'COD',
    subtotal: 100,
    shippingFee: 20,
    total: 110,
    voucherCode: 'DEMO10',
    discount: 10,
    createdAt: '2026-10-10T00:00:00Z',
    history: [
      {
        status: 'DELIVERED',
        actor: 'ADMIN',
        occurredAt: '2026-10-10T00:00:00Z',
        imported: false,
      },
    ],
    items: [
      {
        productId: 2,
        productName: 'Điện thoại',
        variantId: 3,
        variantName: 'Đen',
        sku: 'PHONE-BLACK',
        unitPrice: 100,
        quantity: 1,
        lineTotal: 100,
      },
    ],
  };
  function setup(adminMode: boolean, status = order.status) {
    const current = { ...order, status };
    const orders = {
      findAdminOrder: vi.fn(() => of(current)),
      findById: vi.fn(() => of(current)),
      confirmReceipt: vi.fn(() => of<Order>({ ...order, status: 'COMPLETED' })),
      cancel: vi.fn(),
    };
    const catalog = {
      orderReviewStatuses: vi.fn(() =>
        of([{ productId: 2, variantId: 3, reviewed: true }]),
      ),
    };
    TestBed.configureTestingModule({
      imports: [OrderDetailComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              data: { adminMode },
              paramMap: convertToParamMap({ id: '1' }),
            },
          },
        },
        { provide: OrderService, useValue: orders },
        { provide: CatalogService, useValue: catalog },
        { provide: CartService, useValue: { addOrder: vi.fn() } },
      ],
    });
    const fixture = TestBed.createComponent(OrderDetailComponent);
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance, orders, catalog };
  }
  it('loads admin endpoint and hides customer receipt confirmation', () => {
    const { fixture, component, orders } = setup(true);
    expect(orders.findAdminOrder).toHaveBeenCalledWith(1);
    expect(orders.findById).not.toHaveBeenCalled();
    expect(
      fixture.nativeElement.querySelector('table[mat-table]'),
    ).toBeTruthy();
    expect(fixture.nativeElement.textContent).not.toContain(
      'Đã nhận được hàng',
    );
    component.confirmReceipt();
    expect(orders.confirmReceipt).not.toHaveBeenCalled();
  });
  it('shows receipt confirmation only for a delivered customer order', () => {
    const { fixture, component, orders } = setup(false);
    expect(fixture.nativeElement.textContent).toContain('Đã nhận được hàng');
    component.confirmReceipt();
    fixture.detectChanges();
    expect(orders.confirmReceipt).toHaveBeenCalledWith(1);
    expect(component.order?.status).toBe('COMPLETED');
    expect(component.itemColumns).toContain('review');
    expect(fixture.nativeElement.textContent).toContain('Đã đánh giá');
  });
  it('blocks duplicate confirmations and preserves delivered status on failure', () => {
    const { component, orders } = setup(false);
    const pending = new Subject<Order>();
    orders.confirmReceipt.mockReturnValue(pending);
    component.confirmReceipt();
    component.confirmReceipt();
    expect(orders.confirmReceipt).toHaveBeenCalledTimes(1);
    pending.error(new Error('timeout'));
    expect(component.confirming).toBe(false);
    expect(component.order?.status).toBe('DELIVERED');
    expect(component.confirmationError).not.toBe('');
  });
  it('shows voucher discount and persisted history without inventing events', () => {
    const { fixture, component } = setup(true, 'COMPLETED');
    expect(fixture.nativeElement.textContent).toContain('Giảm giá DEMO10');
    expect(fixture.nativeElement.querySelectorAll('.timeline li').length).toBe(
      1,
    );
    expect(component.itemColumns).not.toContain('review');
  });
});
