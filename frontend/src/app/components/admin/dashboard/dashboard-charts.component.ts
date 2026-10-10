import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  Input,
  OnChanges,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { DashboardReport } from '../../../responses/dashboard.response';
import { ChartPoint } from '../../../models/chart-point.model';
import { ChartTick } from '../../../models/chart-tick.model';
import { ChartBar } from '../../../models/chart-bar.model';
import { ChartSegment } from '../../../models/chart-segment.model';
import { ORDER_STATUS_LABELS, OrderStatus } from '../../../models/order.model';

@Component({
  selector: 'app-dashboard-charts',
  imports: [CommonModule, RouterLink, MatCardModule],
  templateUrl: './dashboard-charts.component.html',
  styleUrl: './dashboard-charts.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardChartsComponent implements OnChanges {
  @Input({ required: true }) report!: DashboardReport;
  readonly circumference = 2 * Math.PI * 72;
  readonly statusColors: Record<OrderStatus, string> = {
    PENDING: '#b45309',
    CONFIRMED: '#2563eb',
    SHIPPING: '#7c3aed',
    DELIVERED: '#0891b2',
    COMPLETED: '#0f766e',
    CANCELLED: '#dc2626',
  };
  points: ChartPoint[] = [];
  revenueTicks: ChartTick[] = [];
  dateTicks: ChartTick[] = [];
  quantityTicks: ChartTick[] = [];
  bars: ChartBar[] = [];
  segments: ChartSegment[] = [];
  linePoints = '';
  totalOrders = 0;
  hasRevenueData = false;

  ngOnChanges(): void {
    this.buildRevenue();
    this.buildProducts();
    this.buildStatuses();
  }

  private buildRevenue(): void {
    const days = [...this.report.dailyRevenue].sort((a, b) =>
      a.date.localeCompare(b.date),
    );
    const maximum = Math.max(1, ...days.map((day) => day.revenue));
    this.hasRevenueData = days.some((day) => day.completedOrders > 0);
    this.points = days.map((day, index) => ({
      ...day,
      x: days.length === 1 ? 340 : 80 + (index / (days.length - 1)) * 520,
      y: 240 - (day.revenue / maximum) * 200,
      label: `${this.dateLabel(day.date)}: ${this.money(day.revenue)}, ${day.completedOrders} đơn hoàn thành`,
    }));
    this.linePoints = this.points
      .map((point) => `${point.x},${point.y}`)
      .join(' ');
    this.revenueTicks = Array.from({ length: 5 }, (_, index) => ({
      position: 240 - index * 50,
      label: this.compact((maximum * index) / 4),
    }));
    const indexes = [
      ...new Set(
        Array.from({ length: Math.min(5, days.length) }, (_, index) =>
          days.length === 1
            ? 0
            : Math.round(
                (index * (days.length - 1)) / (Math.min(5, days.length) - 1),
              ),
        ),
      ),
    ];
    this.dateTicks = indexes.map((index) => ({
      position: this.points[index].x,
      label: this.dateLabel(days[index].date),
    }));
  }

  private buildProducts(): void {
    const products = [...this.report.topProducts]
      .sort((a, b) => b.quantity - a.quantity || a.productId - b.productId)
      .slice(0, 5);
    const maximum = Math.max(1, ...products.map((product) => product.quantity));
    const step = 520 / Math.max(1, products.length);
    this.bars = products.map((product, index) => ({
      productId: product.productId,
      name: product.productName,
      quantity: product.quantity,
      x: 80 + index * step + step * 0.2,
      y: 240 - (product.quantity / maximum) * 180,
      width: step * 0.6,
      height: (product.quantity / maximum) * 180,
      label: `${product.productName}: ${product.quantity} sản phẩm đã bán`,
    }));
    const values = [
      ...new Set(
        Array.from({ length: 5 }, (_, index) =>
          Math.round((maximum * index) / 4),
        ),
      ),
    ];
    this.quantityTicks = values.map((value) => ({
      position: 240 - (value / maximum) * 180,
      label: String(value),
    }));
  }

  private buildStatuses(): void {
    const statuses = Object.keys(ORDER_STATUS_LABELS) as OrderStatus[];
    this.totalOrders = statuses.reduce(
      (total, status) => total + (this.report.statusCounts[status] ?? 0),
      0,
    );
    let offset = 0;
    this.segments = statuses.map((status) => {
      const count = this.report.statusCounts[status] ?? 0;
      const percentage = this.totalOrders
        ? (count / this.totalOrders) * 100
        : 0;
      const length = (percentage / 100) * this.circumference;
      const segment: ChartSegment = {
        status,
        label: ORDER_STATUS_LABELS[status],
        count,
        percentage,
        color: this.statusColors[status],
        dashArray: `${length} ${this.circumference - length}`,
        dashOffset: -offset,
      };
      offset += length;
      return segment;
    });
  }

  private dateLabel(date: string): string {
    return `${date.slice(8, 10)}/${date.slice(5, 7)}`;
  }

  private money(value: number): string {
    return new Intl.NumberFormat('vi-VN', {
      style: 'currency',
      currency: 'VND',
      maximumFractionDigits: 0,
    }).format(value);
  }

  private compact(value: number): string {
    return new Intl.NumberFormat('vi-VN', {
      notation: 'compact',
      maximumFractionDigits: 1,
    }).format(value);
  }
}
