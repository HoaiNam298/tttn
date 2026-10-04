import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { CreateOrderPayload } from '../dtos/create-order.dto';
import { environment } from '../environments/environment';
import { Order } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  create(payload: CreateOrderPayload): Observable<Order> {
    return this.http.post<Order>(`${environment.apiUrl}/orders`, payload);
  }

  findById(id: number): Observable<Order> {
    return this.http.get<Order>(`${environment.apiUrl}/orders/${id}`);
  }
}
