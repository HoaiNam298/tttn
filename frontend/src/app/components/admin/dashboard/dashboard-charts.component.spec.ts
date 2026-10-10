import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { DashboardReport } from '../../../responses/dashboard.response';
import { DashboardChartsComponent } from './dashboard-charts.component';

function report(): DashboardReport {
  return {
    from: '2026-10-01',
    to: '2026-10-02',
    timezone: 'Asia/Ho_Chi_Minh',
    revenue: 0,
    completedOrders: 0,
    estimatedCompletionCount: 0,
    productCount: 200,
    categoryCount: 10,
    orderCount: 0,
    statusCounts: {},
    dailyRevenue: [],
    topProducts: [],
    recentOrders: [],
  };
}

describe('Dashboard charts', () => {
  it('renders explicit empty states without inventing chart data', async () => {
    await TestBed.configureTestingModule({
      imports: [DashboardChartsComponent],
      providers: [provideRouter([])],
    }).compileComponents();
    const fixture = TestBed.createComponent(DashboardChartsComponent);
    fixture.componentRef.setInput('report', report());
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.chart-empty').length).toBe(
      3,
    );
    expect(fixture.nativeElement.querySelectorAll('.plot, .donut').length).toBe(
      0,
    );
    expect(fixture.nativeElement.textContent).toContain('Tất cả thời gian');
  });

  it('handles a single completed day with zero revenue without NaN or Infinity', () => {
    const component = new DashboardChartsComponent();
    component.report = {
      ...report(),
      dailyRevenue: [{ date: '2026-10-01', revenue: 0, completedOrders: 1 }],
    };
    component.ngOnChanges();
    expect(component.hasRevenueData).toBe(true);
    expect(component.points[0].x).toBe(340);
    expect(component.points[0].y).toBe(240);
    expect(component.linePoints).toBe('340,240');
    expect(component.dateTicks).toHaveLength(1);
  });

  it('sorts days without mutating the report and puts the largest revenue at the top', () => {
    const component = new DashboardChartsComponent();
    const dailyRevenue = [
      { date: '2026-10-02', revenue: 200000, completedOrders: 2 },
      { date: '2026-10-01', revenue: 0, completedOrders: 0 },
    ];
    component.report = { ...report(), dailyRevenue };
    component.ngOnChanges();
    expect(dailyRevenue[0].date).toBe('2026-10-02');
    expect(component.points.map((point) => point.date)).toEqual([
      '2026-10-01',
      '2026-10-02',
    ]);
    expect(component.points[1].y).toBe(40);
    expect(component.dateTicks.map((tick) => tick.label)).toEqual([
      '01/10',
      '02/10',
    ]);
  });

  it('calculates donut proportions from actual status counts with consecutive offsets', () => {
    const component = new DashboardChartsComponent();
    component.report = {
      ...report(),
      statusCounts: { PENDING: 1, COMPLETED: 3 },
    };
    component.ngOnChanges();
    expect(component.totalOrders).toBe(4);
    const pending = component.segments.find(
      (segment) => segment.status === 'PENDING',
    )!;
    const completed = component.segments.find(
      (segment) => segment.status === 'COMPLETED',
    )!;
    expect(pending.percentage).toBe(25);
    expect(completed.percentage).toBe(75);
    expect(completed.dashOffset).toBeCloseTo(-component.circumference / 4);
    expect(
      component.segments.reduce((sum, segment) => sum + segment.percentage, 0),
    ).toBe(100);
  });

  it('uses quantity rather than monetary value to rank product bars and resets on a new report', () => {
    const component = new DashboardChartsComponent();
    component.report = {
      ...report(),
      topProducts: [
        {
          productId: 1,
          productName: 'Expensive',
          quantity: 1,
          grossSales: 9000000,
        },
        {
          productId: 2,
          productName: 'Popular',
          quantity: 3,
          grossSales: 30000,
        },
      ],
    };
    component.ngOnChanges();
    expect(component.bars.map((bar) => bar.productId)).toEqual([2, 1]);
    expect(component.bars[0].height).toBe(180);
    expect(
      new Set(component.quantityTicks.map((tick) => tick.label)).size,
    ).toBe(component.quantityTicks.length);
    component.report = report();
    component.ngOnChanges();
    expect(component.bars).toEqual([]);
    expect(component.linePoints).toBe('');
    expect(component.totalOrders).toBe(0);
  });
});
