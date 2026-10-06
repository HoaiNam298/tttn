import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { AddressPayload } from '../dtos/address.dto';
import { Address } from '../models/address.model';
import { environment } from '../environments/environment';

@Injectable({ providedIn: 'root' })
export class AddressService {
  private readonly http = inject(HttpClient);
  private readonly url = `${environment.apiUrl}/addresses`;

  findMine(): Observable<Address[]> {
    return this.http.get<Address[]>(this.url);
  }
  save(value: AddressPayload, id?: number): Observable<Address> {
    return id
      ? this.http.put<Address>(`${this.url}/${id}`, value)
      : this.http.post<Address>(this.url, value);
  }
  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}
