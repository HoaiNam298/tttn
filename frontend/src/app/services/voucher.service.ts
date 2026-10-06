import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { Voucher } from '../models/voucher.model';
import { VoucherPayload } from '../dtos/voucher.dto';
import { PageResponse } from '../responses/page.response';

@Injectable({ providedIn: 'root' })
export class VoucherService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/admin/vouchers`;

  findAll(page = 0): Observable<PageResponse<Voucher>> {
    return this.http.get<PageResponse<Voucher>>(this.url, {
      params: { page, size: 10 },
    });
  }

  save(payload: VoucherPayload, id?: number): Observable<Voucher> {
    return id == null
      ? this.http.post<Voucher>(this.url, payload)
      : this.http.put<Voucher>(`${this.url}/${id}`, payload);
  }
}
