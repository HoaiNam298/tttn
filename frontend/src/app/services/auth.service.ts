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
    const session = this.session();
    if (!session || this.isExpired(session.accessToken)) {
      this.clearSession();
      return null;
    }
    return session.accessToken;
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

  clearSession(): void {
    localStorage.removeItem(this.key);
  }

  private isExpired(token: string): boolean {
    try {
      const encodedPayload = token.split('.')[1];
      const normalizedPayload = encodedPayload
        .replace(/-/g, '+')
        .replace(/_/g, '/');
      const paddedPayload = normalizedPayload.padEnd(
        Math.ceil(normalizedPayload.length / 4) * 4,
        '=',
      );
      const payload = JSON.parse(atob(paddedPayload)) as { exp?: number };
      return !payload.exp || payload.exp * 1000 <= Date.now();
    } catch {
      return true;
    }
  }
}
