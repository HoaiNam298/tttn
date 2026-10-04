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

@Injectable({ providedIn: 'root' })
export class CatalogService {
  private readonly http = inject(HttpClient);
  private readonly api = environment.apiUrl;

  categories(): Observable<Category[]> {
    return this.http.get<Category[]>(`${this.api}/categories`);
  }
  product(id: number): Observable<Product> {
    return this.http.get<Product>(`${this.api}/products/${id}`);
  }
  reviews(id: number, page = 0): Observable<ProductReviewOverview> {
    return this.http.get<ProductReviewOverview>(
      `${this.api}/products/${id}/reviews`,
      {
        params: { page, size: 10 },
      },
    );
  }
  createReview(
    productId: number,
    orderId: number,
    rating: number,
    comment: string,
  ): Observable<ProductReview> {
    return this.http.post<ProductReview>(
      `${this.api}/products/${productId}/reviews`,
      {
        orderId,
        rating,
        comment,
      },
    );
  }
  products(
    keyword = '',
    categoryId?: number,
    page = 0,
    size = 12,
  ): Observable<PageResponse<Product>> {
    let params = new HttpParams()
      .set('keyword', keyword)
      .set('page', page)
      .set('size', size);
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
