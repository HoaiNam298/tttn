import { HttpClient } from '@angular/common/http';
import { effect, inject, Injectable, signal } from '@angular/core';
import { catchError, Observable, of, switchMap, timer } from 'rxjs';
import { AuthService } from './auth.service';
import { environment } from '../environments/environment';
import { ShopNotification } from '../models/notification.model';
import { PageResponse } from '../responses/page.response';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);
  private readonly auth = inject(AuthService);
  private readonly api = `${environment.apiUrl}/notifications`;
  readonly unread = signal(0);

  constructor() {
    effect((onCleanup) => {
      const userId = this.auth.userId();
      this.unread.set(0);
      if (userId === null) {
        return;
      }
      const subscription = timer(0, 30000)
        .pipe(
          switchMap(() =>
            this.http
              .get<number>(`${this.api}/unread-count`)
              .pipe(catchError(() => of(0))),
          ),
        )
        .subscribe((count) => {
          if (this.auth.userId() === userId) {
            this.unread.set(count);
          }
        });
      onCleanup(() => subscription.unsubscribe());
    });
  }

  list(page = 0): Observable<PageResponse<ShopNotification>> {
    return this.http.get<PageResponse<ShopNotification>>(this.api, {
      params: { page, size: 10 },
    });
  }

  markRead(id: number): Observable<void> {
    return this.http.put<void>(`${this.api}/${id}/read`, {});
  }

  markAllRead(): Observable<void> {
    return this.http.put<void>(`${this.api}/read-all`, {});
  }

  refresh(): Observable<number> {
    return this.http.get<number>(`${this.api}/unread-count`);
  }
}
