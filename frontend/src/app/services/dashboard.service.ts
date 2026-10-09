import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../environments/environment';
import { DashboardReport } from '../responses/dashboard.response';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  report(from: string, to: string): Observable<DashboardReport> {
    return this.http.get<DashboardReport>(
      `${environment.apiUrl}/admin/dashboard`,
      { params: { from, to } },
    );
  }
}
