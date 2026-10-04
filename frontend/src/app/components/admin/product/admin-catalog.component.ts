import { CommonModule } from '@angular/common';
import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ProductPayload } from '../../../dtos/product-payload.dto';
import { Category } from '../../../models/category.model';
import { Product } from '../../../models/product.model';
import { AuthService } from '../../../services/auth.service';
import { CatalogService } from '../../../services/catalog.service';
import { CategoryManagementComponent } from '../category/category-management.component';

@Component({
  selector: 'app-admin-catalog',
  imports: [CommonModule, ReactiveFormsModule, CategoryManagementComponent],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './admin-catalog.component.html',
})
export class AdminCatalogComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  readonly auth = inject(AuthService);

  products: Product[] = [];
  categories: Category[] = [];
  editingId?: number;
  message = '';
  error = '';
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
    this.catalog.categories().subscribe((value) => (this.categories = value));
    this.catalog
      .products('', undefined, 0, 100)
      .subscribe((value) => (this.products = value.content));
  }
  saveProduct(): void {
    if (this.productForm.invalid) {
      this.productForm.markAllAsTouched();
      return;
    }
    const payload = this.productForm.getRawValue() as ProductPayload;
    const request = this.editingId
      ? this.catalog.updateProduct(this.editingId, payload)
      : this.catalog.createProduct(payload);
    request.subscribe({
      next: () => {
        this.message = this.editingId
          ? 'Đã cập nhật sản phẩm.'
          : 'Đã tạo sản phẩm.';
        this.cancelEdit();
        this.reload();
      },
      error: () => {
        this.error = 'Không thể lưu sản phẩm.';
      },
    });
  }
  editProduct(product: Product): void {
    this.editingId = product.id;
    this.productForm.setValue({
      name: product.name,
      price: product.price,
      thumbnail: product.thumbnail ?? '',
      description: product.description,
      categoryId: product.category.id,
    });
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }
  cancelEdit(): void {
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
        next: () => this.reload(),
        error: () => {
          this.error = 'Không thể xóa sản phẩm.';
        },
      });
    }
  }

  logout(): void {
    this.auth.logout().subscribe(() => window.location.assign('/login'));
  }
}
