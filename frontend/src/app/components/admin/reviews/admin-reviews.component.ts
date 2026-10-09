import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  ElementRef,
  inject,
  OnInit,
  ViewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../../../services/catalog.service';
import { ProductReview } from '../../../models/product.model';

@Component({
  selector: 'app-admin-reviews',
  imports: [CommonModule, ReactiveFormsModule, RouterLink],
  templateUrl: './admin-reviews.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class AdminReviewsComponent implements OnInit {
  private readonly catalog = inject(CatalogService);
  private readonly destroyRef = inject(DestroyRef);
  @ViewChild('replyDialog') private dialog?: ElementRef<HTMLDialogElement>;
  readonly form = inject(FormBuilder).nonNullable.group({
    reply: ['', [Validators.required, Validators.maxLength(1000)]],
  });
  reviews: ProductReview[] = [];
  selected?: ProductReview;
  page = 0;
  totalPages = 0;
  loading = false;
  saving = false;
  error = '';
  replyError = '';

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    if (this.loading) {
      return;
    }
    this.loading = true;
    this.error = '';
    this.catalog
      .adminReviews(page)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          this.reviews = result.content;
          this.page = result.number;
          this.totalPages = result.totalPages;
          this.loading = false;
        },
        error: () => {
          this.error = 'Không tải được đánh giá.';
          this.loading = false;
        },
      });
  }

  open(review: ProductReview): void {
    this.selected = review;
    this.replyError = '';
    this.form.reset({ reply: review.shopReply ?? '' });
    this.dialog?.nativeElement.showModal();
  }

  close(): void {
    if (!this.saving) {
      this.dialog?.nativeElement.close();
      this.selected = undefined;
    }
  }

  save(): void {
    const reply = this.form.getRawValue().reply.trim();
    if (!this.selected || this.saving || this.form.invalid || !reply) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.catalog
      .replyToReview(this.selected.id, reply, this.selected.version ?? 0)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving = false;
          this.close();
          this.load(this.page);
        },
        error: () => {
          this.saving = false;
          this.replyError =
            'Không lưu được phản hồi. Nếu đánh giá đã thay đổi, đóng popup và tải lại danh sách.';
        },
      });
  }
}
