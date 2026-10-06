import {
  Component,
  EventEmitter,
  OnInit,
  Output,
  inject,
  ChangeDetectionStrategy,
  ElementRef,
  ViewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { Category } from '../../../models/category.model';
import { CatalogService } from '../../../services/catalog.service';

@Component({
  selector: 'app-category-management',
  imports: [ReactiveFormsModule, FormsModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './category-management.component.html',
})
export class CategoryManagementComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  @ViewChild('editor') private editor!: ElementRef<HTMLDialogElement>;
  formError = '';
  message = '';

  openCreate(): void {
    this.cancelEdit();
    this.editor.nativeElement.showModal();
  }

  onDialogCancel(event: Event): void {
    event.preventDefault();
    this.cancelEdit();
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
    request.subscribe({
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
    this.formError = '';
    this.editingId = category.id;
    this.form.setValue({ name: category.name });
    this.editor.nativeElement.showModal();
  }

  cancelEdit(): void {
    if (this.saving) {
      return;
    }
    this.editor.nativeElement.close();
    this.formError = '';
    this.editingId = undefined;
    this.form.reset();
  }

  delete(category: Category): void {
    if (!confirm(`Xóa danh mục "${category.name}"?`)) {
      return;
    }

    this.catalog.deleteCategory(category.id).subscribe({
      next: () => this.afterChange(),
      error: () => {
        this.error = 'Không thể xóa danh mục đang có sản phẩm.';
      },
    });
  }

  load(page = 0): void {
    this.loading = true;
    this.catalog
      .categoryPage(this.keyword.trim(), page, this.pageSize)
      .subscribe({
        next: (response) => {
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
          this.loading = false;
          this.error = 'Không thể tải danh mục.';
        },
      });
  }

  private afterChange(): void {
    this.load(this.page);
    this.changed.emit();
  }
}
