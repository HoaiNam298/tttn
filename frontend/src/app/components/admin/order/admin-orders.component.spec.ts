import { TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter,
} from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { OrderService } from '../../../services/order.service';
import { OrderSummary } from '../../../models/order.model';
import { AdminOrdersComponent } from './admin-orders.component';

describe('Material order management', () => {
  const order: OrderSummary = {
    id: 1,
    orderNumber: 'ORD-1',
    recipientName: 'Khách hàng',
    phoneNumber: '0901234567',
    status: 'PENDING',
    total: 100,
    createdAt: '2026-10-10T00:00:00Z',
  };
  const page = { content: [order], number: 0, totalPages: 1, totalElements: 1 };

  function setup() {
    const service = {
      findAll: vi.fn(() => of(page)),
      updateStatus: vi.fn(() => of(order)),
    };
    TestBed.configureTestingModule({
      imports: [AdminOrdersComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({ status: 'PENDING' }),
            },
          },
        },
        { provide: OrderService, useValue: service },
      ],
    });
    const fixture = TestBed.createComponent(AdminOrdersComponent);
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance, service };
  }

  it('uses Vietnamese labels, Material pagination and status from dashboard link', () => {
    const { fixture, component, service } = setup();
    expect(fixture.nativeElement.textContent).toContain('Chờ xác nhận');
    expect(fixture.nativeElement.querySelector('mat-paginator')).toBeTruthy();
    component.changePage({ pageIndex: 0, pageSize: 20, length: 1 });
    expect(service.findAll).toHaveBeenLastCalledWith('PENDING', 0, 20);
  });

  it('does not allow the admin to complete a delivered order', () => {
    const { component, service } = setup();
    expect(component.nextStatuses('DELIVERED')).toEqual([]);
    component.update({ ...order, status: 'DELIVERED' }, 'COMPLETED');
    expect(service.updateStatus).not.toHaveBeenCalled();
  });

  it('blocks duplicate updates while pending and unlocks the row on error', () => {
    const { component, service } = setup();
    const pending = new Subject<OrderSummary>();
    service.updateStatus.mockReturnValue(pending);
    component.update(order, 'CONFIRMED');
    component.update(order, 'CONFIRMED');
    expect(service.updateStatus).toHaveBeenCalledTimes(1);
    expect(component.updating.has(order.id)).toBe(true);
    pending.error(new Error('conflict'));
    expect(component.updating.has(order.id)).toBe(false);
    expect(component.error).not.toBe('');
    service.updateStatus.mockReturnValue(
      throwError(() => new Error('timeout')),
    );
    component.update(order, 'CONFIRMED');
    expect(component.updating.size).toBe(0);
  });

  it('ignores stale responses when the status filter changes quickly', () => {
    const { component, service } = setup();
    const oldResponse = new Subject<typeof page>();
    const newResponse = new Subject<typeof page>();
    service.findAll
      .mockReturnValueOnce(oldResponse)
      .mockReturnValueOnce(newResponse);
    component.load();
    component.selectedStatus = 'SHIPPING';
    component.load();
    newResponse.next({ ...page, content: [{ ...order, status: 'SHIPPING' }] });
    oldResponse.next(page);
    expect(component.items[0].status).toBe('SHIPPING');
    expect(component.loading).toBe(false);
  });
});
