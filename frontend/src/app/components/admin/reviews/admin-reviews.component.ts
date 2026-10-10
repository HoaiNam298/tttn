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
import { createMaterialPaginatorIntl } from '../../shared/material-paginator-intl';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  TemplateRef,
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
  providers: [
    { provide: MatPaginatorIntl, useFactory: createMaterialPaginatorIntl },
  ],
  styleUrl: './admin-reviews.component.scss',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatPaginatorModule,
    MatProgressBarModule,
    MatDialogModule,
  ],
  templateUrl: './admin-reviews.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class AdminReviewsComponent implements OnInit {
  private readonly catalog = inject(CatalogService);
  private readonly destroyRef = inject(DestroyRef);

  @ViewChild('replyDialog') private editor!: TemplateRef<unknown>;
  private readonly dialogs = inject(MatDialog);
  private editorRef?: MatDialogRef<unknown>;
  constructor() {
    this.destroyRef.onDestroy(() => this.editorRef?.close());
  }
  private showEditor(): void {
    const ref = this.dialogs.open(this.editor, {
      width: '600px',
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

  readonly form = inject(FormBuilder).nonNullable.group({
    reply: ['', [Validators.required, Validators.maxLength(1000)]],
  });
  reviews: ProductReview[] = [];
  selected?: ProductReview;
  page = 0;
  pageSize = 10;
  totalElements = 0;

  changePage(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.load(event.pageIndex);
  }

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
      .adminReviews(page, this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          if (page > 0 && page >= result.totalPages) {
            this.loading = false;
            this.load(Math.max(0, result.totalPages - 1));
            return;
          }
          this.reviews = result.content;
          this.page = result.number;
          this.totalPages = result.totalPages;
          this.totalElements = result.totalElements;
          this.loading = false;
        },
        error: () => {
          this.error = 'Không tải được đánh giá.';
          this.loading = false;
        },
      });
  }

  open(review: ProductReview): void {
    if (this.saving || this.editorRef) {
      return;
    }
    this.selected = review;
    this.replyError = '';
    this.form.reset({ reply: review.shopReply ?? '' });
    this.showEditor();
  }

  close(): void {
    if (!this.saving) {
      this.editorRef?.close();
      this.editorRef = undefined;
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
