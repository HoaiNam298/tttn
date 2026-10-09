import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { Product } from '../models/product.model';
import { PageResponse } from '../responses/page.response';
import { FavoriteState } from '../responses/favorite-state.response';

@Injectable({ providedIn: 'root' })
export class FavoriteService {
  private readonly http = inject(HttpClient);
  private readonly api = `${environment.apiUrl}/favorites`;

  list(page = 0): Observable<PageResponse<Product>> {
    return this.http.get<PageResponse<Product>>(this.api, {
      params: { page, size: 12 },
    });
  }

  state(productId: number): Observable<FavoriteState> {
    return this.http.get<FavoriteState>(`${this.api}/${productId}`);
  }

  set(productId: number, favorite: boolean): Observable<void> {
    return favorite
      ? this.http.put<void>(`${this.api}/${productId}`, {})
      : this.http.delete<void>(`${this.api}/${productId}`);
  }
}
