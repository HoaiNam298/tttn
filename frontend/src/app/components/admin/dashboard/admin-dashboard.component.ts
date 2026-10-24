import { ShopDatePipe } from '../../shared/shop-date.pipe';
import { ShopNumberPipe } from '../../shared/shop-number.pipe';
import { ShopMoneyPipe } from '../../shared/shop-money.pipe';
import { ErrorMessageService } from '../../../services/error-message.service';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { effect } from '@angular/core';
import { createMaterialPaginatorIntl } from '../../shared/material-paginator-intl';
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
import { MatTooltipModule } from '@angular/material/tooltip';
import { DailyRevenue } from '../../../models/daily-revenue.model';
import { DashboardService } from '../../../services/dashboard.service';
import { DashboardReport } from '../../../responses/dashboard.response';
import { ORDER_STATUSES, OrderStatus } from '../../../models/order.model';
import { DashboardChartsComponent } from './dashboard-charts.component';

function createPaginatorIntl(): MatPaginatorIntl {
  const intl = createMaterialPaginatorIntl();
  const translate = inject(TranslateService);
  effect(() => {
    translate.currentLang();
    intl.itemsPerPageLabel = translate.instant('admin.daysPerPage');
    intl.changes.next();
  });
  intl.getRangeLabel = (page, size, length): string =>
    translate.instant('admin.dayRange', {
      start: length && size ? page * size + 1 : 0,
      end: length && size ? Math.min((page + 1) * size, length) : 0,
      total: length,
    });
  return intl;
}

@Component({
  selector: 'app-admin-dashboard',
  imports: [
    ShopDatePipe,
    ShopNumberPipe,
    ShopMoneyPipe,
    TranslatePipe,
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
    MatTooltipModule,
  ],
  providers: [{ provide: MatPaginatorIntl, useFactory: createPaginatorIntl }],
  templateUrl: './admin-dashboard.component.html',
  styleUrl: './admin-dashboard.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class AdminDashboardComponent implements OnInit {
  private readonly errorMessages = inject(ErrorMessageService);
  private readonly translate = inject(TranslateService);
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
  get labels(): Record<OrderStatus, string> {
    return Object.fromEntries(
      ORDER_STATUSES.map((status) => [
        status,
        this.translate.instant('orderStatus.' + status),
      ]),
    ) as Record<OrderStatus, string>;
  }
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
      this.error = this.translate.instant(
        'admin.selectAValidDateRangeOfUpTo93',
      );
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
        error: (error) => {
          if (revision === this.revision) {
            this.error = this.errorMessages.message(
              error,
              'admin.cannotLoadTheReportPleaseTryAgain',
            );
            this.loading = false;
          }
        },
      });
  }
}
