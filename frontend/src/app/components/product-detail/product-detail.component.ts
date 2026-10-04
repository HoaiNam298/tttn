import { CommonModule } from '@angular/common';
import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { Router } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { CatalogService } from '../../services/catalog.service';
import { Product, ProductReview } from '../../models/product.model';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-product-detail',
  imports: [CommonModule, RouterLink, ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './product-detail.component.html',
})
export class ProductDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

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
  reviewMessage = '';
  reviewError = '';
  reviewForm = this.fb.nonNullable.group({
    rating: [5, [Validators.required, Validators.min(1), Validators.max(5)]],
    comment: ['', [Validators.required, Validators.maxLength(1000)]],
  });
  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    const orderId = Number(this.route.snapshot.queryParamMap.get('orderId'));
    this.orderId =
      Number.isInteger(orderId) && orderId > 0 ? orderId : undefined;
    this.catalog.product(id).subscribe({
      next: (value) => {
        this.product = value;
        this.selectedImage = value.images?.[0] ?? value.thumbnail ?? '';
      },
      error: () => (this.error = 'Không tìm thấy sản phẩm.'),
    });
    this.loadReviews(id);
  }

  addToCart(): void {
    if (!this.product) {
      return;
    }
    this.cart.add(this.product.id, this.quantity);
    this.added = true;
  }

  buyNow(): void {
    this.addToCart();
    void this.router.navigate(['/checkout']);
  }

  loadReviews(productId: number, page = 0): void {
    this.catalog.reviews(productId, page).subscribe((overview) => {
      this.reviews = overview.reviews.content;
      this.averageRating = overview.averageRating;
      this.totalReviews = overview.totalReviews;
      this.reviewPage = overview.reviews.number;
      this.reviewTotalPages = overview.reviews.totalPages;
    });
  }

  submitReview(): void {
    if (!this.product || !this.orderId || this.reviewForm.invalid) {
      this.reviewForm.markAllAsTouched();
      return;
    }
    const value = this.reviewForm.getRawValue();
    this.catalog
      .createReview(this.product.id, this.orderId, value.rating, value.comment)
      .subscribe({
        next: () => {
          this.reviewMessage = 'Cảm ơn bạn đã đánh giá sản phẩm.';
          this.reviewError = '';
          this.orderId = undefined;
          this.loadReviews(this.product!.id);
        },
        error: () =>
          (this.reviewError =
            'Không thể gửi đánh giá. Đơn phải đã giao và chưa được đánh giá.'),
      });
  }

  starText(rating: number): string {
    const rounded = Math.max(0, Math.min(5, Math.round(rating)));
    return '★'.repeat(rounded) + '☆'.repeat(5 - rounded);
  }
}
