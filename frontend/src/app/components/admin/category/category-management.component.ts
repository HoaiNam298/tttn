import {
  Component,
  EventEmitter,
  OnInit,
  Output,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Category } from '../../../models/category.model';
import { CatalogService } from '../../../services/catalog.service';

@Component({
  selector: 'app-category-management',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './category-management.component.html',
})
export class CategoryManagementComponent implements OnInit {
  private readonly formBuilder = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);

  @Output() readonly changed = new EventEmitter<void>();

  categories: Category[] = [];
  error = '';
  form = this.formBuilder.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(100)]],
  });

  ngOnInit(): void {
    this.load();
  }

  create(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.catalog.createCategory(this.form.controls.name.value).subscribe({
      next: () => {
        this.form.reset();
        this.afterChange();
      },
      error: () => {
        this.error = 'Không thể tạo danh mục.';
      },
    });
  }

  edit(category: Category): void {
    const name = prompt('Tên danh mục', category.name)?.trim();
    if (!name || name === category.name) {
      return;
    }

    this.catalog.updateCategory(category.id, name).subscribe({
      next: () => this.afterChange(),
      error: () => {
        this.error = 'Không thể cập nhật danh mục.';
      },
    });
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

  private load(): void {
    this.catalog.categories().subscribe({
      next: (categories) => {
        this.categories = categories;
        this.error = '';
      },
      error: () => {
        this.error = 'Không thể tải danh mục.';
      },
    });
  }

  private afterChange(): void {
    this.load();
    this.changed.emit();
  }
}
