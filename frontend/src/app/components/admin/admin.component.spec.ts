import { NgZone, provideZoneChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { Subject } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { DashboardService } from '../../services/dashboard.service';
import { AdminComponent } from './admin.component';
import { AdminDashboardComponent } from './dashboard/admin-dashboard.component';

describe('Admin dashboard asynchronous rendering', () => {
  it('renders API results without requiring another user interaction', async () => {
    const report = new Subject<unknown>();
    await TestBed.configureTestingModule({
      imports: [AdminComponent],
      providers: [
        provideZoneChangeDetection(),
        provideRouter([
          { path: 'dashboard', component: AdminDashboardComponent },
        ]),
        { provide: AuthService, useValue: { session: () => null } },
        {
          provide: DashboardService,
          useValue: { report: () => report },
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
        report.next({
          productCount: 200,
          categoryCount: 10,
          orderCount: 4,
          statusCounts: { PENDING: 2 },
          revenue: 0,
          completedOrders: 0,
          estimatedCompletionCount: 0,
          from: '2026-10-01',
          to: '2026-10-09',
          dailyRevenue: [],
          topProducts: [],
          recentOrders: [],
        });
        report.complete();
      }, 0);
    });
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).not.toContain('Đang tải số liệu');
    expect(fixture.nativeElement.textContent).toContain('200');
    expect(fixture.nativeElement.querySelectorAll('.stat-card').length).toBe(7);
  });
});
