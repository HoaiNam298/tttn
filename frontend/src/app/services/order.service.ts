import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateOrderPayload } from '../dtos/create-order.dto';
import { environment } from '../environments/environment';
import { Order, OrderStatus, OrderSummary } from '../models/order.model';
import { PageResponse } from '../responses/page.response';
import { CheckoutQuote } from '../responses/checkout-quote.response';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  create(payload: CreateOrderPayload): Observable<Order> {
    return this.http.post<Order>(`${environment.apiUrl}/orders`, payload);
  }

  quote(payload: CreateOrderPayload): Observable<CheckoutQuote> {
    return this.http.post<CheckoutQuote>(
      `${environment.apiUrl}/orders/quote`,
      payload,
    );
  }

  cancel(id: number): Observable<Order> {
    return this.http.post<Order>(
      `${environment.apiUrl}/orders/${id}/cancel`,
      {},
    );
  }

  findById(id: number): Observable<Order> {
    return this.http.get<Order>(`${environment.apiUrl}/orders/${id}`);
  }

  confirmReceipt(id: number): Observable<Order> {
    return this.http.post<Order>(
      `${environment.apiUrl}/orders/${id}/confirm-receipt`,
      {},
    );
  }

  findAdminOrder(id: number): Observable<Order> {
    return this.http.get<Order>(`${environment.apiUrl}/admin/orders/${id}`);
  }

  findMine(
    page = 0,
    size = 10,
    status: OrderStatus | '' = '',
  ): Observable<PageResponse<OrderSummary>> {
    const params: Record<string, string | number> = { page, size };
    if (status) {
      params['status'] = status;
    }
    return this.http.get<PageResponse<OrderSummary>>(
      `${environment.apiUrl}/orders`,
      { params },
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
