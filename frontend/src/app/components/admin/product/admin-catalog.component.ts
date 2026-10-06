import { CommonModule } from '@angular/common';
import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
  ElementRef,
  ViewChild,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { FormsModule } from '@angular/forms';
import { ProductPayload } from '../../../dtos/product-payload.dto';
import { Category } from '../../../models/category.model';
import { Product } from '../../../models/product.model';
import { CatalogService } from '../../../services/catalog.service';
import { ProductInventoryEditorComponent } from './product-inventory-editor.component';

@Component({
  selector: 'app-admin-catalog',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    ProductInventoryEditorComponent,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './admin-catalog.component.html',
})
export class AdminCatalogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  @ViewChild('editor') private editor!: ElementRef<HTMLDialogElement>;

  products: Product[] = [];
  categories: Category[] = [];
  editingId?: number;
  message = '';
  error = '';
  page = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;
  keyword = '';
  categoryId?: number;
  loading = false;
  saving = false;
  formError = '';

  openCreate(): void {
    this.cancelEdit();
    this.editor.nativeElement.showModal();
  }

  onDialogCancel(event: Event): void {
    event.preventDefault();
    this.cancelEdit();
  }
  productForm = this.fb.nonNullable.group({
    name: ['', [Validators.required, Validators.maxLength(350)]],
    price: [0, [Validators.required, Validators.min(0)]],
    thumbnail: ['', Validators.maxLength(500)],
    description: ['', Validators.maxLength(10000)],
    categoryId: [0, Validators.min(1)],
  });
  ngOnInit(): void {
    this.reload();
  }
  reload(): void {
    this.catalog.categories().subscribe({
      next: (value) => {
        this.categories = value;
      },
      error: () => {
        this.error = 'Không thể tải danh mục.';
      },
    });
    this.load();
  }

  load(page = 0): void {
    this.loading = true;
    this.error = '';
    this.catalog
      .products(this.keyword.trim(), this.categoryId, page, this.pageSize)
      .subscribe({
        next: (value) => {
          if (value.totalPages > 0 && page >= value.totalPages) {
            this.load(value.totalPages - 1);
            return;
          }
          this.products = value.content;
          this.page = value.number;
          this.totalPages = value.totalPages;
          this.totalElements = value.totalElements;
          this.loading = false;
        },
        error: () => {
          this.error = 'Không thể tải sản phẩm.';
          this.loading = false;
        },
      });
  }
  saveProduct(): void {
    if (this.saving) {
      return;
    }
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }
    const payload = this.productForm.getRawValue() as ProductPayload;
    this.saving = true;
    this.formError = '';
    const request = this.editingId
      ? this.catalog.updateProduct(this.editingId, payload)
      : this.catalog.createProduct(payload);
    request.subscribe({
      next: () => {
        this.saving = false;
        this.message = this.editingId
          ? 'Đã cập nhật sản phẩm.'
          : 'Đã tạo sản phẩm.';
        this.cancelEdit();
        this.load(this.page);
      },
      error: () => {
        this.saving = false;
        this.formError =
          'Không thể lưu sản phẩm. Vui lòng kiểm tra dữ liệu và thử lại.';
      },
    });
  }
  editProduct(product: Product): void {
    this.formError = '';
    this.editingId = product.id;
    this.productForm.setValue({
      name: product.name,
      price: product.price,
      thumbnail: product.thumbnail ?? '',
      description: product.description,
      categoryId: product.category.id,
    });
    this.editor.nativeElement.showModal();
  }
  cancelEdit(): void {
    if (this.saving) {
      return;
    }
    this.editor.nativeElement.close();
    this.formError = '';
    this.editingId = undefined;
    this.productForm.reset({
      name: '',
      price: 0,
      thumbnail: '',
      description: '',
      categoryId: 0,
    });
  }
  deleteProduct(product: Product): void {
    if (confirm(`Xóa sản phẩm "${product.name}"?`)) {
      this.catalog.deleteProduct(product.id).subscribe({
        next: () => this.load(this.page),
        error: () => {
          this.error = 'Không thể xóa sản phẩm.';
        },
      });
    }
  }
}
