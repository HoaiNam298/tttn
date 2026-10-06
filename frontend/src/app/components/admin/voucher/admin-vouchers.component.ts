import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
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
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './admin-vouchers.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class AdminVouchersComponent implements OnInit {
  private readonly service = inject(VoucherService);
  private readonly fb = inject(FormBuilder);
  @ViewChild('editor') private editor!: ElementRef<HTMLDialogElement>;
  result?: PageResponse<Voucher>;
  editing?: Voucher;
  page = 0;
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
    this.service.findAll(page).subscribe({
      next: (result) => {
        this.result = result;
        this.page = page;
        this.loading = false;
      },
      error: () => {
        this.error = 'Không tải được voucher.';
        this.loading = false;
      },
    });
  }

  open(voucher?: Voucher): void {
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
    this.editor.nativeElement.showModal();
  }

  close(event?: Event): void {
    event?.preventDefault();
    if (!this.saving) {
      this.editor.nativeElement.close();
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
      .subscribe({
        next: () => {
          this.saving = false;
          this.close();
          this.load();
        },
        error: (response) => {
          this.saving = false;
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
