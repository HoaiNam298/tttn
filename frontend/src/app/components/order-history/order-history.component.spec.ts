import { TestBed } from '@angular/core/testing';
import { of, throwError } from 'rxjs';
import { OrderService } from '../../services/order.service';
import { OrderHistoryComponent } from './order-history.component';

describe('Order status tabs', () => {
  it('resets pagination and recovers after an API failure when changing tabs', () => {
    const findMine = vi
      .fn()
      .mockReturnValueOnce(throwError(() => new Error('offline')))
      .mockReturnValue(of({ content: [], number: 0, totalPages: 0 }));
    TestBed.configureTestingModule({
      providers: [{ provide: OrderService, useValue: { findMine } }],
    });
    const component = TestBed.runInInjectionContext(
      () => new OrderHistoryComponent(),
    );
    component.ngOnInit();
    expect(component.error).toBeTruthy();
    component.page = 4;
    component.selectStatus('SHIPPING');
    expect(findMine).toHaveBeenLastCalledWith(0, 10, 'SHIPPING');
    expect(component.page).toBe(0);
    expect(component.error).toBe('');
  });
});
