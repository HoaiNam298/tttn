import {
  ChangeDetectionStrategy,
  Component,
  TemplateRef,
  DestroyRef,
  ViewChild,
  inject,
  output,
} from '@angular/core';
import {
  FormArray,
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { CatalogService } from '../../../services/catalog.service';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import {
  MatDialog,
  MatDialogModule,
  MatDialogRef,
} from '@angular/material/dialog';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { Product } from '../../../models/product.model';
import { ProductVariant } from '../../../models/product-variant.model';
import { ProductVariantPayload } from '../../../dtos/product-inventory.dto';

@Component({
  selector: 'app-product-inventory-editor',
  imports: [
    ReactiveFormsModule,
    MatButtonModule,
    MatCheckboxModule,
    MatDialogModule,
    MatExpansionModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './product-inventory-editor.component.html',
  styleUrl: './product-inventory-editor.component.scss',
})
export class ProductInventoryEditorComponent {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  @ViewChild('dialog') private editor!: TemplateRef<unknown>;
  private readonly dialogs = inject(MatDialog);
  private readonly destroyRef = inject(DestroyRef);
  private dialogRef?: MatDialogRef<unknown>;
  constructor() {
    this.destroyRef.onDestroy(() => this.dialogRef?.close());
  }
  get busy(): boolean {
    return this.loading || this.saving || this.uploading;
  }
  readonly saved = output<void>();
  product?: Product;
  images: string[] = [];
  loading = false;
  saving = false;
  uploading = false;
  error = '';
  readonly variants = new FormArray<
    ReturnType<ProductInventoryEditorComponent['variantGroup']>
  >([]);
  readonly form = this.fb.group({
    stock: this.fb.nonNullable.control(0, [
      Validators.required,
      Validators.min(0),
      Validators.max(1000000),
      Validators.pattern(/^\d+$/),
    ]),
    variants: this.variants,
  });

  variantGroup(value?: ProductVariant) {
    return this.fb.group({
      id: [value?.id ?? null],
      sku: this.fb.nonNullable.control(value?.sku ?? '', [
        Validators.required,
        Validators.maxLength(64),
        Validators.pattern(/^[A-Za-z0-9_-]+$/),
      ]),
      name: this.fb.nonNullable.control(value?.name ?? '', [
        Validators.required,
        Validators.maxLength(150),
      ]),
      color: this.fb.nonNullable.control(
        value?.color ?? '',
        Validators.maxLength(60),
      ),
      size: this.fb.nonNullable.control(
        value?.size ?? '',
        Validators.maxLength(60),
      ),
      capacity: this.fb.nonNullable.control(
        value?.capacity ?? '',
        Validators.maxLength(60),
      ),
      price: this.fb.nonNullable.control(value?.price ?? 0, [
        Validators.required,
        Validators.min(0),
        Validators.max(9999999999.99),
      ]),
      stock: this.fb.nonNullable.control(value?.stock ?? 0, [
        Validators.required,
        Validators.min(0),
        Validators.max(1000000),
        Validators.pattern(/^\d+$/),
      ]),
      imageUrl: this.fb.nonNullable.control(
        value?.imageUrl ?? '',
        Validators.maxLength(500),
      ),
      active: this.fb.nonNullable.control(value?.active ?? true),
    });
  }

  open(id: number): void {
    if (this.dialogRef || this.busy) {
      return;
    }
    this.product = undefined;
    this.error = '';
    this.loading = true;
    this.variants.clear();
    this.images = [];
    this.form.enable({ emitEvent: false });
    const ref = this.dialogs.open(this.editor, {
      width: '1000px',
      maxWidth: 'calc(100vw - 32px)',
      disableClose: true,
    });
    this.dialogRef = ref;
    ref
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.dialogRef === ref) {
          this.dialogRef = undefined;
        }
      });
    this.catalog
      .product(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (product) => {
          this.product = product;
          this.images = [
            ...new Set(
              [...(product.images ?? []), product.thumbnail].filter(
                (url): url is string => !!url,
              ),
            ),
          ];
          this.form.controls.stock.setValue(product.stock ?? 0);
          product.variants?.forEach((variant) =>
            this.variants.push(this.variantGroup(variant)),
          );
          this.loading = false;
        },
        error: () => {
          this.error = 'Không tải được dữ liệu sản phẩm.';
          this.loading = false;
        },
      });
  }

  close(): void {
    if (this.saving || this.uploading || this.loading) {
      return;
    }
    this.dialogRef?.close();
    this.dialogRef = undefined;
  }

  addVariant(): void {
    if (!this.busy && this.variants.length < 50) {
      this.variants.push(this.variantGroup());
    }
  }

  removeVariant(index: number): void {
    if (this.busy || index < 0 || index >= this.variants.length) {
      return;
    }
    if (this.variants.at(index).controls.id.value == null) {
      this.variants.removeAt(index);
    } else {
      this.variants.at(index).controls.active.setValue(false);
    }
  }

  upload(event: Event, variantIndex?: number): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (
      !file ||
      this.busy ||
      (variantIndex == null && this.images.length >= 10)
    ) {
      return;
    }
    if (
      variantIndex != null &&
      (variantIndex < 0 || variantIndex >= this.variants.length)
    ) {
      return;
    }
    if (
      !['image/jpeg', 'image/png'].includes(file.type) ||
      file.size > 5 * 1024 * 1024
    ) {
      this.error = 'Chọn ảnh JPEG/PNG tối đa 5 MB.';
      input.value = '';
      return;
    }
    this.uploading = true;
    this.form.disable({ emitEvent: false });
    this.error = '';
    this.catalog
      .uploadImage(file)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          if (variantIndex != null) {
            this.variants
              .at(variantIndex)
              .controls.imageUrl.setValue(result.url);
          } else if (this.images.length < 10) {
            this.images.push(result.url);
          }
          this.uploading = false;
          this.form.enable({ emitEvent: false });
          input.value = '';
        },
        error: () => {
          this.error = 'Upload thất bại. Kiểm tra ảnh hoặc kết nối.';
          this.uploading = false;
          this.form.enable({ emitEvent: false });
          input.value = '';
        },
      });
  }

  makePrimary(index: number): void {
    if (this.busy || index < 0 || index >= this.images.length) {
      return;
    }
    const [image] = this.images.splice(index, 1);
    this.images.unshift(image);
  }

  removeImage(index: number): void {
    if (!this.busy && index >= 0 && index < this.images.length) {
      this.images.splice(index, 1);
    }
  }

  save(): void {
    this.form.markAllAsTouched();
    if (!this.product || this.form.invalid || this.saving || this.uploading) {
      return;
    }
    const variants: ProductVariantPayload[] = this.variants
      .getRawValue()
      .map((value) => ({
        ...value,
        id: value.id ?? undefined,
        name: value.name.trim(),
        sku: value.sku.trim(),
      }));
    const stock = this.form.controls.stock.value;
    this.saving = true;
    this.form.disable({ emitEvent: false });
    this.error = '';
    this.catalog
      .updateInventory(this.product.id, {
        version: this.product.version ?? 0,
        stock,
        variants,
        images: [...this.images],
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving = false;
          this.form.enable({ emitEvent: false });
          this.close();
          this.saved.emit();
        },
        error: (response) => {
          this.saving = false;
          this.form.enable({ emitEvent: false });
          this.error =
            response.error?.message ??
            'Không lưu được ảnh/SKU. Hãy mở lại dữ liệu nếu tồn kho vừa thay đổi.';
        },
      });
  }
}
