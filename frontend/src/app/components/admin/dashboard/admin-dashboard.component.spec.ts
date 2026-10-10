import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { By } from '@angular/platform-browser';
import { of } from 'rxjs';
import { DashboardService } from '../../../services/dashboard.service';
import { DashboardReport } from '../../../responses/dashboard.response';
import { AdminDashboardComponent } from './admin-dashboard.component';
import { DashboardChartsComponent } from './dashboard-charts.component';

describe('Dashboard daily revenue pagination', () => {
  const report: DashboardReport = {
    from: '2026-10-01',
    to: '2026-10-30',
    timezone: 'Asia/Ho_Chi_Minh',
    revenue: 300,
    completedOrders: 30,
    estimatedCompletionCount: 0,
    productCount: 200,
    categoryCount: 10,
    orderCount: 30,
    statusCounts: { COMPLETED: 30 },
    dailyRevenue: Array.from({ length: 30 }, (_, i) => ({
      date: `2026-10-${String(i + 1).padStart(2, '0')}`,
      completedOrders: 1,
      revenue: 10,
    })),
    topProducts: [],
    recentOrders: [],
  };

  function setup(): { component: AdminDashboardComponent; requests: string[] } {
    const requests: string[] = [];
    TestBed.configureTestingModule({
      imports: [AdminDashboardComponent],
      providers: [
        provideRouter([]),
        {
          provide: DashboardService,
          useValue: {
            report: (from: string) => {
              requests.push(from);
              return of(report);
            },
          },
        },
      ],
    });
    return {
      component: TestBed.runInInjectionContext(
        () => new AdminDashboardComponent(),
      ),
      requests,
    };
  }

  it('shows seven days initially and does not fetch again when changing page', () => {
    const { component, requests } = setup();
    component.load();
    expect(component.dailyRows).toHaveLength(7);
    component.changeDailyPage({ pageIndex: 1, pageSize: 7, length: 30 });
    expect(component.dailyRows[0].date).toBe('2026-10-08');
    expect(requests).toHaveLength(1);
  });

  it('handles the partial last page and larger page sizes', () => {
    const { component } = setup();
    component.load();
    component.changeDailyPage({ pageIndex: 4, pageSize: 7, length: 30 });
    expect(component.dailyRows).toHaveLength(2);
    component.changeDailyPage({ pageIndex: 0, pageSize: 14, length: 30 });
    expect(component.dailyRows).toHaveLength(14);
    component.changeDailyPage({ pageIndex: 0, pageSize: 30, length: 30 });
    expect(component.dailyRows).toHaveLength(30);
  });

  it('resets the page after a new report while retaining the page size', () => {
    const { component } = setup();
    component.load();
    component.changeDailyPage({ pageIndex: 1, pageSize: 14, length: 30 });
    component.range.setValue({ from: '2026-10-01', to: '2026-10-30' });
    component.load();
    expect(component.dailyPageIndex).toBe(0);
    expect(component.dailyPageSize).toBe(14);
  });

  it('handles empty report data without mutating the source', () => {
    const { component } = setup();
    expect(component.dailyRows).toEqual([]);
    component.data = { ...report, dailyRevenue: [] };
    expect(component.dailyRows).toEqual([]);
    component.data = report;
    component.dailyRows.pop();
    expect(report.dailyRevenue).toHaveLength(30);
  });

  it('keeps full order numbers accessible and distinguishes cancelled order badges', () => {
    setup();
    const fixture = TestBed.createComponent(AdminDashboardComponent);
    fixture.detectChanges();
    const number = 'ee317126-9cc7-4b4a-b469-123456789012';
    fixture.componentInstance.data = {
      ...report,
      recentOrders: [
        {
          id: 1,
          orderNumber: number,
          recipientName: 'Khách hàng',
          phoneNumber: '0901234567',
          createdAt: '2026-10-01T00:00:00Z',
          total: 100,
          status: 'CANCELLED',
        },
      ],
    };
    fixture.detectChanges();
    const link = fixture.nativeElement.querySelector('.order-number');
    expect(link.textContent.trim()).toBe(number);
    expect(link.getAttribute('aria-label')).toContain(number);
    expect(
      fixture.nativeElement.querySelector('.status-CANCELLED').textContent,
    ).toContain('Đã hủy');
  });

  it('renders paginated rows while passing all thirty days to the charts', () => {
    setup();
    const fixture = TestBed.createComponent(AdminDashboardComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('mat-paginator').length).toBe(
      1,
    );
    const charts = fixture.debugElement.query(
      By.directive(DashboardChartsComponent),
    );
    expect(charts.componentInstance.report.dailyRevenue).toHaveLength(30);
    fixture.componentInstance.changeDailyPage({
      pageIndex: 1,
      pageSize: 7,
      length: 30,
    });
    fixture.detectChanges();
    expect(charts.componentInstance.points).toHaveLength(30);
    expect(fixture.componentInstance.dailyRows).toHaveLength(7);
  });
});
