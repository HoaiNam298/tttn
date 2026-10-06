import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
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
import { Product } from '../../../models/product.model';
import { ProductVariant } from '../../../models/product-variant.model';
import { ProductVariantPayload } from '../../../dtos/product-inventory.dto';

@Component({
  selector: 'app-product-inventory-editor',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './product-inventory-editor.component.html',
  styleUrl: './product-inventory-editor.component.scss',
})
export class ProductInventoryEditorComponent {
  private readonly fb = inject(FormBuilder);
  private readonly catalog = inject(CatalogService);
  @ViewChild('dialog') private dialog!: ElementRef<HTMLDialogElement>;
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
    this.product = undefined;
    this.error = '';
    this.loading = true;
    this.variants.clear();
    this.dialog.nativeElement.showModal();
    this.catalog.product(id).subscribe({
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

  close(event?: Event): void {
    event?.preventDefault();
    if (this.saving || this.uploading || this.loading) {
      return;
    }
    this.dialog.nativeElement.close();
  }

  addVariant(): void {
    if (this.variants.length < 50) {
      this.variants.push(this.variantGroup());
    }
  }

  removeVariant(index: number): void {
    if (this.variants.at(index).controls.id.value == null) {
      this.variants.removeAt(index);
    } else {
      this.variants.at(index).controls.active.setValue(false);
    }
  }

  upload(event: Event, variantIndex?: number): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file || this.uploading || this.saving) {
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
    this.error = '';
    this.catalog.uploadImage(file).subscribe({
      next: (result) => {
        if (variantIndex != null) {
          this.variants.at(variantIndex).controls.imageUrl.setValue(result.url);
        } else if (this.images.length < 10) {
          this.images.push(result.url);
        }
        this.uploading = false;
        input.value = '';
      },
      error: () => {
        this.error = 'Upload thất bại. Kiểm tra ảnh hoặc kết nối.';
        this.uploading = false;
        input.value = '';
      },
    });
  }

  makePrimary(index: number): void {
    const [image] = this.images.splice(index, 1);
    this.images.unshift(image);
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
    this.saving = true;
    this.error = '';
    this.catalog
      .updateInventory(this.product.id, {
        version: this.product.version ?? 0,
        stock: this.form.controls.stock.value,
        variants,
        images: this.images,
      })
      .subscribe({
        next: () => {
          this.saving = false;
          this.dialog.nativeElement.close();
          this.saved.emit();
        },
        error: (response) => {
          this.saving = false;
          this.error =
            response.error?.message ??
            'Không lưu được ảnh/SKU. Hãy mở lại dữ liệu nếu tồn kho vừa thay đổi.';
        },
      });
  }
}
