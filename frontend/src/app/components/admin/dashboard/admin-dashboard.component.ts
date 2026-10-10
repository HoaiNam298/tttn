import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import {
  MatPaginatorIntl,
  MatPaginatorModule,
  PageEvent,
} from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatTableModule } from '@angular/material/table';
import { DailyRevenue } from '../../../models/daily-revenue.model';
import { DashboardService } from '../../../services/dashboard.service';
import { DashboardReport } from '../../../responses/dashboard.response';
import { ORDER_STATUS_LABELS, OrderStatus } from '../../../models/order.model';
import { DashboardChartsComponent } from './dashboard-charts.component';

function createPaginatorIntl(): MatPaginatorIntl {
  const intl = new MatPaginatorIntl();
  intl.itemsPerPageLabel = 'Số ngày / trang';
  intl.nextPageLabel = 'Trang sau';
  intl.previousPageLabel = 'Trang trước';
  intl.firstPageLabel = 'Trang đầu';
  intl.lastPageLabel = 'Trang cuối';
  intl.getRangeLabel = (page, size, length): string => {
    if (!length) return '0 / 0 ngày';
    return `${page * size + 1}–${Math.min((page + 1) * size, length)} / ${length} ngày`;
  };
  return intl;
}

@Component({
  selector: 'app-admin-dashboard',
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    DashboardChartsComponent,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTableModule,
  ],
  providers: [{ provide: MatPaginatorIntl, useFactory: createPaginatorIntl }],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class AdminDashboardComponent implements OnInit {
  private readonly service = inject(DashboardService);
  private readonly destroyRef = inject(DestroyRef);
  private revision = 0;
  private readonly today = new Date().toLocaleDateString('sv-SE', {
    timeZone: 'Asia/Ho_Chi_Minh',
  });
  readonly range = inject(FormBuilder).nonNullable.group({
    from: [
      new Date(new Date(`${this.today}T00:00:00Z`).getTime() - 29 * 86400000)
        .toISOString()
        .slice(0, 10),
      Validators.required,
    ],
    to: [this.today, Validators.required],
  });
  readonly labels = ORDER_STATUS_LABELS;
  readonly processingStates = [
    'PENDING',
    'CONFIRMED',
    'SHIPPING',
    'DELIVERED',
  ] as const;
  data?: DashboardReport;
  loading = false;
  error = '';
  dailyPageIndex = 0;
  dailyPageSize = 7;
  readonly dailyColumns = ['date', 'orders', 'revenue'];
  readonly productColumns = ['product', 'quantity', 'grossSales'];
  readonly recentColumns = [
    'number',
    'recipient',
    'createdAt',
    'total',
    'status',
  ];

  get dailyRows(): DailyRevenue[] {
    const start = this.dailyPageIndex * this.dailyPageSize;
    return (this.data?.dailyRevenue ?? []).slice(
      start,
      start + this.dailyPageSize,
    );
  }

  changeDailyPage(event: PageEvent): void {
    this.dailyPageIndex = event.pageIndex;
    this.dailyPageSize = event.pageSize;
  }

  statusLabel(status: OrderStatus): string {
    return this.labels[status];
  }

  ngOnInit(): void {
    this.load();
  }

  load(): void {
    if (this.range.invalid) {
      this.range.markAllAsTouched();
      return;
    }
    const { from, to } = this.range.getRawValue();
    const days = (Date.parse(to) - Date.parse(from)) / 86400000 + 1;
    if (!Number.isFinite(days) || days < 1 || days > 93) {
      this.error = 'Chọn khoảng ngày hợp lệ, tối đa 93 ngày.';
      return;
    }
    const revision = ++this.revision;
    this.loading = true;
    this.error = '';
    this.service
      .report(from, to)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (report) => {
          if (revision === this.revision) {
            this.data = report;
            this.dailyPageIndex = 0;
            this.loading = false;
          }
        },
        error: () => {
          if (revision === this.revision) {
            this.error = 'Không tải được báo cáo. Vui lòng thử lại.';
            this.loading = false;
          }
        },
      });
  }
}
