import { NgZone, provideZoneChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { CatalogService } from '../../services/catalog.service';
import { OrderService } from '../../services/order.service';
import { AdminComponent } from './admin.component';
import { AdminDashboardComponent } from './dashboard/admin-dashboard.component';

describe('Admin dashboard asynchronous rendering', () => {
  it('renders API results without requiring another user interaction', async () => {
    const products = new Subject<unknown>();
    const categories = new Subject<unknown>();
    const orders = new Subject<unknown>();
    const pending = new Subject<unknown>();
    await TestBed.configureTestingModule({
      imports: [AdminComponent],
      providers: [
        provideZoneChangeDetection(),
        provideRouter([
          { path: 'dashboard', component: AdminDashboardComponent },
        ]),
        { provide: AuthService, useValue: { session: () => null } },
        {
          provide: CatalogService,
          useValue: {
            products: () => products,
            categoryPage: () => categories,
          },
        },
        {
          provide: OrderService,
          useValue: {
            findAll: (status: string) => (status ? pending : orders),
          },
        },
      ],
    }).compileComponents();
    const fixture = TestBed.createComponent(AdminComponent);
    fixture.autoDetectChanges();
    await TestBed.inject(Router).navigateByUrl('/dashboard');
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('Đang tải số liệu');
    TestBed.inject(NgZone).run(() => {
      setTimeout(() => {
        products.next({ totalElements: 200 });
        products.complete();
        categories.next({ totalElements: 10 });
        categories.complete();
        orders.next({ totalElements: 4, content: [] });
        orders.complete();
        pending.next({ totalElements: 2 });
        pending.complete();
      }, 0);
    });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).not.toContain('Đang tải số liệu');
    expect(fixture.nativeElement.textContent).toContain('200');
    expect(fixture.nativeElement.querySelectorAll('.stat-card').length).toBe(4);
  });
});
