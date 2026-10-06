import { CommonModule } from '@angular/common';
import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
  DestroyRef,
  ElementRef,
  ViewChild,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CatalogService } from '../../services/catalog.service';
import { Product, ProductReview } from '../../models/product.model';
import { CartService } from '../../services/cart.service';
import { EMPTY, catchError, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ProductCardComponent } from '../shared/product-card.component';
import { ProductVariant } from '../../models/product-variant.model';

@Component({
  selector: 'app-product-detail',
  imports: [
    CommonModule,
    RouterLink,
    ReactiveFormsModule,
    ProductCardComponent,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './product-detail.component.html',
})
export class ProductDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private currentProductId = 0;
  @ViewChild('imagePreview')
  private imagePreview?: ElementRef<HTMLDialogElement>;
  relatedProducts: Product[] = [];
  relatedError = '';
  reviewLoadError = '';
  selectedVariantId?: number;

  get selectedVariant(): ProductVariant | undefined {
    return this.product?.variants?.find(
      (variant) => variant.id === this.selectedVariantId,
    );
  }

  get availableVariants(): ProductVariant[] {
    return this.product?.variants?.filter((variant) => variant.active) ?? [];
  }

  get purchasePrice(): number {
    return this.selectedVariant?.price ?? this.product?.price ?? 0;
  }

  get purchaseStock(): number {
    return (this.product?.variants?.length ?? 0) > 0
      ? (this.selectedVariant?.stock ?? 0)
      : (this.product?.stock ?? 0);
  }

  selectVariant(variant: ProductVariant): void {
    this.selectedVariantId = variant.id;
    this.quantity = 1;
    this.added = false;
    this.selectedImage = variant.imageUrl || this.images[0] || '';
  }

  get images(): string[] {
    return [
      ...new Set(
        [
          this.product?.thumbnail,
          ...(this.product?.images ?? []),
          this.selectedVariant?.imageUrl,
        ].filter((image): image is string => !!image),
      ),
    ];
  }

  openImage(): void {
    this.imagePreview?.nativeElement.showModal();
  }

  closeImage(): void {
    this.imagePreview?.nativeElement.close();
  }

  moveImage(offset: number): void {
    const images = this.images;
    if (images.length) {
      this.selectedImage =
        images[
          (images.indexOf(this.selectedImage) + offset + images.length) %
            images.length
        ];
    }
  }

  product?: Product;
  error = '';
  added = false;
  selectedImage = '';
  quantity = 1;
  reviews: ProductReview[] = [];
  averageRating = 0;
  totalReviews = 0;
  reviewPage = 0;
  reviewTotalPages = 0;
  orderId?: number;
  reviewVariantId?: number;
  reviewMessage = '';
  reviewError = '';
  reviewForm = this.fb.nonNullable.group({
    rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', [Validators.required, Validators.maxLength(1000)]],
  });
  ngOnInit(): void {
    this.route.queryParamMap
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((params) => {
        const reviewVariantId = Number(params.get('variantId'));
        this.reviewVariantId =
          Number.isInteger(reviewVariantId) && reviewVariantId > 0
            ? reviewVariantId
            : undefined;
        const orderId = Number(params.get('orderId'));
        this.orderId =
          Number.isInteger(orderId) && orderId > 0 ? orderId : undefined;
      });
    this.route.paramMap
      .pipe(
        switchMap((params) => {
          const id = Number(params.get('id'));
          this.currentProductId = id;
          this.product = undefined;
          this.quantity = 1;
          this.selectedVariantId = undefined;
          this.added = false;
          this.error = '';
          this.relatedProducts = [];
          this.relatedError = '';
          this.reviews = [];
          this.averageRating = 0;
          this.totalReviews = 0;
          this.reviewTotalPages = 0;
          this.reviewMessage = '';
          this.reviewError = '';
          this.closeImage();
          this.loadReviews(id);
          return this.catalog.product(id).pipe(
            catchError(() => {
              this.error = 'Không tìm thấy sản phẩm.';
              return EMPTY;
            }),
          );
        }),
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        this.product = value;
        this.selectedImage = value.thumbnail || value.images?.[0] || '';
        this.catalog
          .products('', value.category.id, 0, 7)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: (page) => {
              if (this.currentProductId === value.id) {
                this.relatedProducts = page.content
                  .filter((item) => item.id !== value.id)
                  .slice(0, 6);
              }
            },
            error: () => {
              if (this.currentProductId === value.id) {
                this.relatedError = 'Chưa tải được sản phẩm liên quan.';
              }
            },
          });
      });
  }

  addToCart(): void {
    if (!this.product || this.purchaseStock < this.quantity) {
      return;
    }
    this.cart.add(this.product.id, this.quantity, this.selectedVariantId);
    this.added = true;
  }

  buyNow(): void {
    if (!this.product || this.purchaseStock < this.quantity) {
      return;
    }
    void this.router.navigate(['/checkout'], {
      queryParams: {
        productId: this.product.id,
        quantity: this.quantity,
        variantId: this.selectedVariantId,
      },
    });
  }

  loadReviews(productId: number, page = 0): void {
    this.reviewLoadError = '';
    this.catalog
      .reviews(productId, page)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (overview) => {
          if (this.currentProductId !== productId) {
            return;
          }
          this.reviews = overview.reviews.content;
          this.averageRating = overview.averageRating;
          this.totalReviews = overview.totalReviews;
          this.reviewPage = overview.reviews.number;
          this.reviewTotalPages = overview.reviews.totalPages;
        },
        error: () => {
          if (this.currentProductId === productId) {
            this.reviewLoadError = 'Chưa tải được đánh giá. Vui lòng thử lại.';
          }
        },
      });
  }

  submitReview(): void {
    if (!this.product || !this.orderId || this.reviewForm.invalid) {
      this.reviewForm.markAllAsTouched();
      return;
    }
    const value = this.reviewForm.getRawValue();
    this.catalog
      .createReview(
        this.product.id,
        this.orderId,
        value.rating,
        value.comment,
        this.reviewVariantId,
      )
      .subscribe({
        next: () => {
          this.reviewMessage = 'Cảm ơn bạn đã đánh giá sản phẩm.';
          this.reviewError = '';
          this.orderId = undefined;
          this.loadReviews(this.product!.id);
        },
        error: () =>
          (this.reviewError =
            'Không thể gửi đánh giá. Bạn phải xác nhận đã nhận hàng và chưa đánh giá sản phẩm này.'),
      });
  }

  starText(rating: number): string {
    const rounded = Math.max(0, Math.min(5, Math.round(rating)));
    return '★'.repeat(rounded) + '☆'.repeat(5 - rounded);
  }
}
