import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateOrderPayload } from '../dtos/create-order.dto';
import { environment } from '../environments/environment';
import { Order, OrderStatus, OrderSummary } from '../models/order.model';
import { PageResponse } from '../responses/page.response';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  create(payload: CreateOrderPayload): Observable<Order> {
    return this.http.post<Order>(`${environment.apiUrl}/orders`, payload);
  }

  findById(id: number): Observable<Order> {
    return this.http.get<Order>(`${environment.apiUrl}/orders/${id}`);
  }

  findMine(page = 0, size = 10): Observable<PageResponse<OrderSummary>> {
    return this.http.get<PageResponse<OrderSummary>>(
      `${environment.apiUrl}/orders`,
      { params: { page, size } },
    );
  }

  findAll(
    status: OrderStatus | '',
    page = 0,
    size = 10,
  ): Observable<PageResponse<OrderSummary>> {
    const params: Record<string, string | number> = { page, size };
    if (status) {
      params['status'] = status;
    }
    return this.http.get<PageResponse<OrderSummary>>(
      `${environment.apiUrl}/admin/orders`,
      { params },
    );
  }

  updateStatus(id: number, status: OrderStatus): Observable<Order> {
    return this.http.patch<Order>(
      `${environment.apiUrl}/admin/orders/${id}/status`,
      { status },
    );
  }
}
