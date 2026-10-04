import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, finalize, of, tap } from 'rxjs';
import { environment } from '../environments/environment';
import { AuthResponse } from '../responses/auth.response';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly key = 'shopapp_auth';

  login(phoneNumber: string, password: string): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${environment.apiUrl}/auth/login`, {
        phoneNumber,
        password,
      })
      .pipe(
        tap((value) => localStorage.setItem(this.key, JSON.stringify(value))),
      );
  }

  session(): AuthResponse | null {
    const value = localStorage.getItem(this.key);
    if (!value) {
      return null;
    }

    try {
      return JSON.parse(value) as AuthResponse;
    } catch {
      localStorage.removeItem(this.key);
      return null;
    }
  }

  token(): string | null {
    return this.session()?.accessToken ?? null;
  }

  isAdmin(): boolean {
    return this.session()?.user.role === 'ADMIN';
  }

  logout(): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/auth/logout`, {}).pipe(
      catchError(() => of(undefined)),
      finalize(() => localStorage.removeItem(this.key)),
    );
  }
}
