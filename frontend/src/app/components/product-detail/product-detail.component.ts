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
import { AuthService } from '../../services/auth.service';
import { FavoriteService } from '../../services/favorite.service';

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
  private readonly auth = inject(AuthService);
  private readonly favorites = inject(FavoriteService);
  favorite = false;
  favoriteBusy = false;
  favoriteError = '';
  private readonly route = inject(ActivatedRoute);
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private currentProductId = 0;
  private reviewRequest = 0;
  private eligibilityRequest = 0;
  reviewRating?: number;
  readonly ratingFilters = [5, 4, 3, 2, 1];
  reviewFiles: File[] = [];
  reviewSaving = false;
  eligibilityLoading = false;
  canReview = false;
  alreadyReviewed = false;
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
        if (this.product) {
          this.checkEligibility();
        }
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
          this.reviewRating = undefined;
          this.reviewFiles = [];
          this.canReview = false;
          this.alreadyReviewed = false;
          this.reviewSaving = false;
          this.reviewForm.reset({ rating: 5, comment: '' });
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
        this.checkEligibility();
        this.favorite = false;
        this.favoriteBusy = false;
        this.favoriteError = '';
        if (this.auth.userId() !== null && this.auth.token()) {
          this.favoriteBusy = true;
          this.favorites
            .state(value.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: (state) => {
                if (this.currentProductId === value.id) {
                  this.favorite = state.favorite;
                  this.favoriteBusy = false;
                }
              },
              error: () => {
                if (this.currentProductId === value.id) {
                  this.favoriteBusy = false;
                  this.favoriteError = 'Không tải được trạng thái yêu thích.';
                }
              },
            });
        }
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

  toggleFavorite(): void {
    if (!this.product || this.favoriteBusy) {
      return;
    }
    if (this.auth.userId() === null || !this.auth.token()) {
      void this.router.navigate(['/login'], {
        queryParams: { returnUrl: `/products/${this.product.id}` },
      });
      return;
    }
    const productId = this.product.id;
    const desired = !this.favorite;
    this.favoriteBusy = true;
    this.favoriteError = '';
    this.favorites
      .set(productId, desired)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          if (this.currentProductId === productId) {
            this.favorite = desired;
            this.favoriteBusy = false;
          }
        },
        error: () => {
          if (this.currentProductId === productId) {
            this.favoriteBusy = false;
            this.favoriteError =
              'Không cập nhật được yêu thích. Vui lòng thử lại.';
          }
        },
      });
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
    const request = ++this.reviewRequest;
    this.reviewLoadError = '';
    this.catalog
      .reviews(productId, page, this.reviewRating)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (overview) => {
          if (
            this.currentProductId !== productId ||
            request !== this.reviewRequest
          ) {
            return;
          }
          this.reviews = overview.reviews.content;
          this.averageRating = overview.averageRating;
          this.totalReviews = overview.totalReviews;
          this.reviewPage = overview.reviews.number;
          this.reviewTotalPages = overview.reviews.totalPages;
        },
        error: () => {
          if (
            this.currentProductId === productId &&
            request === this.reviewRequest
          ) {
            this.reviewLoadError = 'Chưa tải được đánh giá. Vui lòng thử lại.';
          }
        },
      });
  }

  submitReview(): void {
    if (
      !this.product ||
      !this.orderId ||
      this.reviewForm.invalid ||
      !this.canReview ||
      this.reviewSaving
    ) {
      this.reviewForm.markAllAsTouched();
      return;
    }
    const value = this.reviewForm.getRawValue();
    const productId = this.product.id;
    this.reviewSaving = true;
    this.reviewError = '';
    this.catalog
      .createReview(
        this.product.id,
        this.orderId,
        Number(value.rating),
        value.comment,
        this.reviewVariantId,
        this.reviewFiles,
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          if (this.currentProductId !== productId) {
            return;
          }
          this.reviewSaving = false;
          this.canReview = false;
          this.alreadyReviewed = true;
          this.reviewFiles = [];
          this.reviewMessage = 'Cảm ơn bạn đã đánh giá sản phẩm.';
          this.reviewError = '';
          this.orderId = undefined;
          this.loadReviews(this.product!.id);
        },
        error: () => {
          if (this.currentProductId === productId) {
            this.reviewSaving = false;
            this.reviewError =
              'Không thể gửi đánh giá. Vui lòng kiểm tra ảnh hoặc tải lại trạng thái đánh giá.';
            this.checkEligibility();
          }
        },
      });
  }

  filterReviews(rating?: number): void {
    this.reviewRating = rating;
    if (this.product) {
      this.loadReviews(this.product.id);
    }
  }

  selectReviewFiles(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    input.value = '';
    if (
      files.length + this.reviewFiles.length > 5 ||
      files.some(
        (file) =>
          !['image/jpeg', 'image/png'].includes(file.type) ||
          file.size > 5 * 1024 * 1024,
      )
    ) {
      this.reviewError = 'Chọn tối đa 5 ảnh JPEG/PNG, mỗi ảnh không quá 5 MB.';
      return;
    }
    this.reviewError = '';
    this.reviewFiles = [...this.reviewFiles, ...files];
  }

  removeReviewFile(index: number): void {
    this.reviewFiles = this.reviewFiles.filter(
      (_, position) => position !== index,
    );
  }

  private checkEligibility(): void {
    const request = ++this.eligibilityRequest;
    this.canReview = false;
    this.alreadyReviewed = false;
    this.eligibilityLoading = false;
    if (!this.product || !this.orderId) {
      return;
    }
    this.eligibilityLoading = true;
    this.catalog
      .reviewEligibility(this.product.id, this.orderId, this.reviewVariantId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (reviewed) => {
          if (request === this.eligibilityRequest) {
            this.alreadyReviewed = reviewed;
            this.canReview = !reviewed;
            this.eligibilityLoading = false;
          }
        },
        error: () => {
          if (request === this.eligibilityRequest) {
            this.eligibilityLoading = false;
            this.reviewError =
              'Chỉ có thể đánh giá đơn của bạn sau khi xác nhận đã nhận hàng.';
          }
        },
      });
  }

  starText(rating: number): string {
    const rounded = Math.max(0, Math.min(5, Math.round(rating)));
    return '★'.repeat(rounded) + '☆'.repeat(5 - rounded);
  }
}
