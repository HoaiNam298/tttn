import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog.component';
import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  OnInit,
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
import { MatSelectModule } from '@angular/material/select';
import { MatTableModule } from '@angular/material/table';
import {
  MatPaginatorIntl,
  MatPaginatorModule,
  PageEvent,
} from '@angular/material/paginator';
import { createMaterialPaginatorIntl } from '../../shared/material-paginator-intl';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ProductPayload } from '../../../dtos/product-payload.dto';
import { Category } from '../../../models/category.model';
import { Product } from '../../../models/product.model';
import { CatalogService } from '../../../services/catalog.service';
import { ProductInventoryEditorComponent } from './product-inventory-editor.component';

@Component({
  selector: 'app-admin-catalog',
  providers: [
    { provide: MatPaginatorIntl, useFactory: createMaterialPaginatorIntl },
  ],
  styleUrl: './admin-catalog.component.scss',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    FormsModule,
    ProductInventoryEditorComponent,
    MatButtonModule,
    MatCardModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatTableModule,
    MatPaginatorModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './admin-catalog.component.html',
})
export class AdminCatalogComponent implements OnInit {
  private readonly confirmations = inject(MatDialog);
  private confirmationOpen = false;

  private readonly fb = inject(FormBuilder);
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
  readonly columns = ['name', 'category', 'price', 'actions'];

  changePage(event: PageEvent): void {
    this.pageSize = event.pageSize;
    this.load(event.pageIndex);
  }

  private showEditor(): void {
    if (this.saving || this.editorRef) {
      return;
    }
    const ref = this.dialog.open(this.editor, {
      width: '760px',
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
    if (this.saving) {
      return;
    }
    this.cancelEdit();
    this.showEditor();
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
    const revision = ++this.loadRevision;
    this.loading = true;
    this.error = '';
    this.catalog
      .products(this.keyword.trim(), this.categoryId, page, this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (value) => {
          if (revision !== this.loadRevision) {
            return;
          }
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
          if (revision !== this.loadRevision) {
            return;
          }
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
    this.productForm.disable({ emitEvent: false });
    this.formError = '';
    const request = this.editingId
      ? this.catalog.updateProduct(this.editingId, payload)
      : this.catalog.createProduct(payload);
    request.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving = false;
        this.productForm.enable({ emitEvent: false });
        this.message = this.editingId
          ? 'Đã cập nhật sản phẩm.'
          : 'Đã tạo sản phẩm.';
        this.cancelEdit();
        this.load(this.page);
      },
      error: () => {
        this.saving = false;
        this.productForm.enable({ emitEvent: false });
        this.formError =
          'Không thể lưu sản phẩm. Vui lòng kiểm tra dữ liệu và thử lại.';
      },
    });
  }
  editProduct(product: Product): void {
    if (this.saving) {
      return;
    }
    this.formError = '';
    this.editingId = product.id;
    this.productForm.setValue({
      name: product.name,
      price: product.price,
      thumbnail: product.thumbnail ?? '',
      description: product.description,
      categoryId: product.category.id,
    });
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
    this.productForm.reset({
      name: '',
      price: 0,
      thumbnail: '',
      description: '',
      categoryId: 0,
    });
  }
  private deleteProductConfirmed(product: Product): void {
    this.catalog.deleteProduct(product.id).subscribe({
      next: () => this.load(this.page),
      error: () => {
        this.error = 'Không thể xóa sản phẩm.';
      },
    });
  }

  deleteProduct(product: Product): void {
    if (this.confirmationOpen) {
      return;
    }
    this.confirmationOpen = true;
    this.confirmations
      .open(ConfirmationDialogComponent, {
        data: `Xóa sản phẩm "${product.name}"?`,
        width: '440px',
        maxWidth: 'calc(100vw - 32px)',
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed: boolean | undefined) => {
        this.confirmationOpen = false;
        if (confirmed === true) {
          this.deleteProductConfirmed(product);
        }
      });
  }
}
