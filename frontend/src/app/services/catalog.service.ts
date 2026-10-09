import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { ProductPayload } from '../dtos/product-payload.dto';
import { Category } from '../models/category.model';
import {
  Product,
  ProductReview,
  ProductReviewOverview,
} from '../models/product.model';
import { PageResponse } from '../responses/page.response';
import { ProductFilter, PRODUCT_SORT } from '../models/product-filter.model';
import { ProductInventoryPayload } from '../dtos/product-inventory.dto';
import { ImageUploadResponse } from '../responses/image-upload.response';
import { OrderReviewStatus } from '../responses/order-review-status.response';

@Injectable({ providedIn: 'root' })
export class CatalogService {
  orderReviewStatuses(orderId: number): Observable<OrderReviewStatus[]> {
    return this.http.get<OrderReviewStatus[]>(
      `${this.api}/orders/${orderId}/review-statuses`,
    );
  }
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  categories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.api}/categories`);
  }

  categoryPage(
    keyword = '',
    page = 0,
    size = 10,
  ): Observable<PageResponse<Category>> {
    return this.http.get<PageResponse<Category>>(
      `${this.api}/categories/page`,
      { params: { keyword, page, size } },
    );
  }
  product(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.api}/products/${id}`);
  }
  reviews(
    id: number,
    page = 0,
    rating?: number,
  ): Observable<ProductReviewOverview> {
    return this.http.get<ProductReviewOverview>(
      `${this.api}/products/${id}/reviews`,
      {
        params: { page, size: 10, ...(rating ? { rating } : {}) },
      },
    );
  }
  createReview(
    productId: number,
    orderId: number,
    rating: number,
    comment: string,
    variantId?: number,
    images: File[] = [],
  ): Observable<ProductReview> {
    if (images.length) {
      const body = new FormData();
      body.append(
        'review',
        new Blob([JSON.stringify({ orderId, rating, comment, variantId })], {
          type: 'application/json',
        }),
      );
      for (const file of images) {
        body.append('images', file);
      }
      return this.http.post<ProductReview>(
        `${this.api}/products/${productId}/reviews`,
        body,
      );
    }
    return this.http.post<ProductReview>(
      `${this.api}/products/${productId}/reviews`,
      {
        orderId,
        rating,
        comment,
        ...(variantId != null ? { variantId } : {}),
      },
    );
  }
  reviewEligibility(
    productId: number,
    orderId: number,
    variantId?: number,
  ): Observable<boolean> {
    return this.http.get<boolean>(`${this.api}/review-eligibility`, {
      params: { productId, orderId, ...(variantId ? { variantId } : {}) },
    });
  }

  adminReviews(page = 0): Observable<PageResponse<ProductReview>> {
    return this.http.get<PageResponse<ProductReview>>(
      `${this.api}/admin/reviews`,
      { params: { page, size: 10 } },
    );
  }

  replyToReview(
    id: number,
    reply: string,
    version: number,
  ): Observable<ProductReview> {
    return this.http.put<ProductReview>(
      `${this.api}/admin/reviews/${id}/reply`,
      { reply, version },
    );
  }
  products(
    keyword = '',
    categoryId?: number,
    page = 0,
    size = 12,
    filter: ProductFilter = {},
  ): Observable<PageResponse<Product>> {
    let params = new HttpParams()
      .set('keyword', keyword)
      .set('page', page)
      .set('size', size);
    params = params.set('sort', PRODUCT_SORT[filter.sort ?? 'newest']);
    if (filter.minPrice != null) {
      params = params.set('minPrice', filter.minPrice);
    }
    if (filter.maxPrice != null) {
      params = params.set('maxPrice', filter.maxPrice);
    }
    if (categoryId) {
      params = params.set('categoryId', categoryId);
    }
    return this.http.get<PageResponse<Product>>(`${this.api}/products`, {
      params,
    });
  }
  createProduct(value: ProductPayload): Observable<Product> {
    return this.http.post<Product>(`${this.api}/products`, value);
  }

  updateInventory(
    id: number,
    payload: ProductInventoryPayload,
  ): Observable<Product> {
    return this.http.put<Product>(
      `${this.api}/products/${id}/inventory`,
      payload,
    );
  }

  uploadImage(file: File): Observable<ImageUploadResponse> {
    const body = new FormData();
    body.append('file', file);
    return this.http.post<ImageUploadResponse>(`${this.api}/admin/media`, body);
  }
  updateProduct(id: number, value: ProductPayload): Observable<Product> {
    return this.http.put<Product>(`${this.api}/products/${id}`, value);
  }
  deleteProduct(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/products/${id}`);
  }
  createCategory(name: string): Observable<Category> {
    return this.http.post<Category>(`${this.api}/categories`, { name });
  }
  updateCategory(id: number, name: string): Observable<Category> {
    return this.http.put<Category>(`${this.api}/categories/${id}`, { name });
  }
  deleteCategory(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/categories/${id}`);
  }
}
