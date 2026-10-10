import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog.component';
import {
  Component,
  DestroyRef,
  EventEmitter,
  OnInit,
  Output,
  inject,
  ChangeDetectionStrategy,
  TemplateRef,
  ViewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatTableModule } from '@angular/material/table';
import {
  MatPaginatorIntl,
  MatPaginatorModule,
  PageEvent,
} from '@angular/material/paginator';
import { createMaterialPaginatorIntl } from '../../shared/material-paginator-intl';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Category } from '../../../models/category.model';
import { CatalogService } from '../../../services/catalog.service';

@Component({
  selector: 'app-category-management',
  providers: [
    { provide: MatPaginatorIntl, useFactory: createMaterialPaginatorIntl },
  ],
  styleUrl: './category-management.component.scss',
  imports: [
    ReactiveFormsModule,
    FormsModule,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './category-management.component.html',
})
export class CategoryManagementComponent implements OnInit {
  private readonly confirmations = inject(MatDialog);
  private confirmationOpen = false;

  private readonly formBuilder = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  @ViewChild('editor') private editor!: TemplateRef<unknown>;
  private readonly dialog = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private loadRevision = 0;
  private editorRef?: MatDialogRef<unknown>;
  constructor() {
    this.destroyRef.onDestroy(() => {
      this.editorRef?.close();
    });
  }
  readonly columns = ['id', 'name', 'actions'];

  changePage(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.load(event.pageIndex);
  }

  private showEditor(): void {
    if (this.saving || this.editorRef) {
      return;
    }
    const ref = this.dialog.open(this.editor, {
      width: '520px',
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
  formError = '';
  message = '';

  openCreate(): void {
    if (this.saving) {
      return;
    }
    this.cancelEdit();
    this.showEditor();
  }

  @Output() readonly changed = new EventEmitter<void>();

  categories: Category[] = [];
  error = '';
  loading = false;
  saving = false;
  editingId?: number;
  page = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  keyword = '';
  form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.load();
  }

  create(): void {
    if (this.saving) {
      return;
    }
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.saving = true;
    this.formError = '';
    const name = this.form.controls.name.value.trim();
    const request = this.editingId
      ? this.catalog.updateCategory(this.editingId, name)
      : this.catalog.createCategory(name);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving = false;
        this.message = this.editingId
          ? 'Đã cập nhật danh mục.'
          : 'Đã thêm danh mục.';
        this.cancelEdit();
        this.afterChange();
      },
      error: () => {
        this.saving = false;
        this.formError =
          'Không thể lưu danh mục. Kiểm tra tên trùng hoặc không hợp lệ.';
      },
    });
  }

  edit(category: Category): void {
    if (this.saving) {
      return;
    }
    this.formError = '';
    this.editingId = category.id;
    this.form.setValue({ name: category.name });
    this.showEditor();
  }

  cancelEdit(): void {
    if (this.saving) {
      return;
    }
    this.editorRef?.close();
    this.editorRef = undefined;
    this.formError = '';
    this.editingId = undefined;
    this.form.reset();
  }

  private deleteConfirmed(category: Category): void {
    this.catalog.deleteCategory(category.id).subscribe({
      next: () => this.afterChange(),
      error: () => {
        this.error = 'Không thể xóa danh mục đang có sản phẩm.';
      },
    });
  }

  load(page = 0): void {
    const revision = ++this.loadRevision;
    this.loading = true;
    this.catalog
      .categoryPage(this.keyword.trim(), page, this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (response) => {
          if (revision !== this.loadRevision) {
            return;
          }
          if (response.totalPages > 0 && page >= response.totalPages) {
            this.load(response.totalPages - 1);
            return;
          }
          this.categories = response.content;
          this.page = response.number;
          this.totalPages = response.totalPages;
          this.totalElements = response.totalElements;
          this.loading = false;
          this.error = '';
        },
        error: () => {
          if (revision !== this.loadRevision) {
            return;
          }
          this.loading = false;
          this.error = 'Không thể tải danh mục.';
        },
      });
  }

  private afterChange(): void {
    this.load(this.page);
    this.changed.emit();
  }

  delete(category: Category): void {
    if (this.confirmationOpen) {
      return;
    }
    this.confirmationOpen = true;
    this.confirmations
      .open(ConfirmationDialogComponent, {
        data: `Xóa danh mục "${category.name}"?`,
        width: '440px',
        maxWidth: 'calc(100vw - 32px)',
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed: boolean | undefined) => {
        this.confirmationOpen = false;
        if (confirmed === true) {
          this.deleteConfirmed(category);
        }
      });
  }
}
