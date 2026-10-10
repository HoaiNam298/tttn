import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  MatPaginatorIntl,
  MatPaginatorModule,
  PageEvent,
} from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { createMaterialPaginatorIntl } from '../../shared/material-paginator-intl';
import { MatTableModule } from '@angular/material/table';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  TemplateRef,
  OnInit,
  ViewChild,
  inject,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { VoucherService } from '../../../services/voucher.service';
import { Voucher } from '../../../models/voucher.model';
import { PageResponse } from '../../../responses/page.response';

@Component({
  selector: 'app-admin-vouchers',
  providers: [
    { provide: MatPaginatorIntl, useFactory: createMaterialPaginatorIntl },
  ],
  styleUrl: './admin-vouchers.component.scss',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatTableModule,
    MatDialogModule,
    MatCheckboxModule,
  ],
  templateUrl: './admin-vouchers.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class AdminVouchersComponent implements OnInit {
  private readonly service = inject(VoucherService);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);

  @ViewChild('editor') private editor!: TemplateRef<unknown>;
  private readonly dialogs = inject(MatDialog);
  private editorRef?: MatDialogRef<unknown>;
  constructor() {
    this.destroyRef.onDestroy(() => this.editorRef?.close());
  }
  private showEditor(): void {
    const ref = this.dialogs.open(this.editor, {
      width: '800px',
      maxWidth: 'calc(100vw - 32px)',
      disableClose: true,
    });
    this.editorRef = ref;
    ref
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.editorRef === ref) {
          this.editorRef = undefined;
        }
      });
  }

  readonly columns = [
    'code',
    'discount',
    'minimum',
    'usage',
    'expiry',
    'active',
    'actions',
  ];
  result?: PageResponse<Voucher>;
  editing?: Voucher;
  page = 0;
  pageSize = 10;

  changePage(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.load(event.pageIndex);
  }

  loading = false;
  saving = false;
  error = '';
  editorError = '';
  readonly form = this.fb.group({
    code: this.fb.nonNullable.control('', [
      Validators.required,
      Validators.pattern(/^[A-Za-z0-9_-]{3,40}$/),
    ]),
    type: this.fb.nonNullable.control<'FIXED' | 'PERCENT'>('FIXED'),
    amount: this.fb.nonNullable.control(10000, [
      Validators.required,
      Validators.min(0.01),
      Validators.max(9999999999.99),
    ]),
    minimumSubtotal: this.fb.nonNullable.control(0, [
      Validators.required,
      Validators.min(0),
      Validators.max(999999999999.99),
    ]),
    maximumDiscount: this.fb.nonNullable.control(10000, [
      Validators.required,
      Validators.min(0.01),
      Validators.max(9999999999.99),
    ]),
    startsAt: this.fb.nonNullable.control('', Validators.required),
    endsAt: this.fb.nonNullable.control('', Validators.required),
    usageLimit: this.fb.nonNullable.control(100, [
      Validators.required,
      Validators.min(1),
      Validators.max(1000000),
      Validators.pattern(/^\d+$/),
    ]),
    perUserLimit: this.fb.nonNullable.control(1, [
      Validators.required,
      Validators.min(1),
      Validators.max(1000000),
      Validators.pattern(/^\d+$/),
    ]),
    targetUserId: this.fb.control<number | null>(null, [
      Validators.min(1),
      Validators.pattern(/^\d+$/),
    ]),
    active: this.fb.nonNullable.control(true),
  });

  ngOnInit(): void {
    this.load();
  }

  load(page = this.page): void {
    if (this.loading) {
      return;
    }
    this.loading = true;
    this.error = '';
    this.service
      .findAll(page, this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          if (page > 0 && page >= result.totalPages) {
            this.loading = false;
            this.load(Math.max(0, result.totalPages - 1));
            return;
          }
          this.result = result;
          this.page = result.number;
          this.loading = false;
        },
        error: () => {
          this.error = 'Không tải được voucher.';
          this.loading = false;
        },
      });
  }

  open(voucher?: Voucher): void {
    if (this.saving || this.editorRef) {
      return;
    }
    this.editing = voucher;
    this.editorError = '';
    this.form.reset({
      code: voucher?.code ?? '',
      type: voucher?.type ?? 'FIXED',
      amount: voucher?.amount ?? 10000,
      minimumSubtotal: voucher?.minimumSubtotal ?? 0,
      maximumDiscount: voucher?.maximumDiscount ?? 10000,
      startsAt: this.localDate(voucher?.startsAt ?? new Date().toISOString()),
      endsAt: this.localDate(
        voucher?.endsAt ?? new Date(Date.now() + 7 * 86400000).toISOString(),
      ),
      usageLimit: voucher?.usageLimit ?? 100,
      perUserLimit: voucher?.perUserLimit ?? 1,
      targetUserId: voucher?.targetUserId ?? null,
      active: voucher?.active ?? true,
    });
    this.showEditor();
  }

  close(): void {
    if (!this.saving) {
      this.editorRef?.close();
      this.editorRef = undefined;
    }
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.saving) {
      return;
    }
    const value = this.form.getRawValue();
    const start = new Date(value.startsAt);
    const end = new Date(value.endsAt);
    if (
      !Number.isFinite(start.getTime()) ||
      !Number.isFinite(end.getTime()) ||
      end <= start ||
      (value.type === 'PERCENT' && value.amount > 100)
    ) {
      this.editorError =
        'Ngày kết thúc phải sau ngày bắt đầu; phần trăm không vượt quá 100.';
      return;
    }
    this.saving = true;
    this.form.disable({ emitEvent: false });
    this.editorError = '';
    this.service
      .save(
        {
          ...value,
          code: value.code.trim().toUpperCase(),
          startsAt: start.toISOString(),
          endsAt: end.toISOString(),
          version: this.editing?.version,
        },
        this.editing?.id,
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving = false;
          this.form.enable({ emitEvent: false });
          this.close();
          this.load();
        },
        error: (response) => {
          this.saving = false;
          this.form.enable({ emitEvent: false });
          this.editorError =
            response.error?.message ??
            'Không lưu được voucher. Nếu dữ liệu thay đổi, tải lại trước khi sửa.';
        },
      });
  }

  private localDate(instant: string): string {
    const date = new Date(instant);
    return new Date(date.getTime() - date.getTimezoneOffset() * 60000)
      .toISOString()
      .slice(0, 16);
  }
}
