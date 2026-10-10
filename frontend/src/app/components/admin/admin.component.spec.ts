import { NgZone, provideZoneChangeDetection } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { BehaviorSubject, Subject } from 'rxjs';
import { BreakpointObserver } from '@angular/cdk/layout';
import { MatSidenav } from '@angular/material/sidenav';
import { By } from '@angular/platform-browser';
import { vi } from 'vitest';
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

describe('Material admin shell', () => {
  function setup() {
    const screen = new BehaviorSubject({ matches: false, breakpoints: {} });
    const pendingLogout = new Subject<void>();
    const auth = {
      session: () => ({ user: { fullName: 'Nguyễn Hoài Nam' } }),
      logout: vi.fn(() => pendingLogout),
    };
    TestBed.configureTestingModule({
      imports: [AdminComponent],
      providers: [
        provideRouter([
          { path: 'products', children: [] },
          { path: 'login', children: [] },
        ]),
        { provide: AuthService, useValue: auth },
        {
          provide: BreakpointObserver,
          useValue: {
            isMatched: () => false,
            observe: () => screen,
          },
        },
      ],
    });
    const fixture = TestBed.createComponent(AdminComponent);
    fixture.detectChanges();
    const drawer = fixture.debugElement.query(By.directive(MatSidenav))
      .componentInstance as MatSidenav;
    return { fixture, drawer, screen, auth, pendingLogout };
  }

  it('renders six Material navigation links and a desktop side drawer', async () => {
    const { fixture, drawer } = setup();
    await fixture.whenStable();
    expect(drawer.mode).toBe('side');
    expect(drawer.opened).toBe(true);
    expect(
      fixture.nativeElement.querySelectorAll('mat-nav-list a').length,
    ).toBe(6);
    expect(fixture.nativeElement.querySelector('mat-toolbar')).toBeTruthy();
    expect(fixture.componentInstance.initials).toBe('HN');
  });

  it('switches to an overlay drawer on mobile and closes after navigation', async () => {
    const { fixture, drawer, screen } = setup();
    screen.next({ matches: true, breakpoints: {} });
    fixture.detectChanges();
    await fixture.whenStable();
    expect(drawer.mode).toBe('over');
    expect(drawer.opened).toBe(false);
    const button = fixture.nativeElement.querySelector(
      '[aria-controls="admin-navigation"]',
    );
    expect(button).toBeTruthy();
    button.click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(drawer.opened).toBe(true);
    await TestBed.inject(Router).navigateByUrl('/products');
    fixture.detectChanges();
    await fixture.whenStable();
    expect(drawer.opened).toBe(false);
  });

  it('opens the Material account menu with a logout action', async () => {
    const { fixture } = setup();
    fixture.nativeElement.querySelector('.account-button').click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(document.querySelector('[role="menu"]')?.textContent).toContain(
      'Đăng xuất',
    );
  });

  it('blocks duplicate logout requests and allows retry after a failure', () => {
    const { fixture, auth, pendingLogout } = setup();
    fixture.componentInstance.logout();
    fixture.componentInstance.logout();
    expect(auth.logout).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.loggingOut).toBe(true);
    pendingLogout.error(new Error('network error'));
    expect(fixture.componentInstance.loggingOut).toBe(false);
  });
});
