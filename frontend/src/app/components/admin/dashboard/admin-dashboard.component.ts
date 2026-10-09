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
import { DashboardService } from '../../../services/dashboard.service';
import { DashboardReport } from '../../../responses/dashboard.response';
import { ORDER_STATUS_LABELS } from '../../../models/order.model';

@Component({
  selector: 'app-admin-dashboard',
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  templateUrl: './admin-dashboard.component.html',
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
