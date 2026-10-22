import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { environment } from '../environments/environment';

export const authInterceptor: HttpInterceptorFn = (request, next) => {
  const ownApi = request.url.startsWith(`${environment.apiUrl}/`);
  const registration = /\/auth\/register(?:\?|$)/.test(request.url);
  if (!ownApi || registration) {
    return next(request);
  }
  const auth = inject(AuthService);
  const router = inject(Router);
  const initialUserId = auth.session()?.user.id ?? null;
  const sameAccount = () => (auth.session()?.user.id ?? null) === initialUserId;
  const token = auth.token();
  const send = (accessToken: string | null) =>
    next(
      accessToken
        ? request.clone({
            setHeaders: { Authorization: `Bearer ${accessToken}` },
          })
        : request,
    );
  const refreshAndSend = () =>
    auth.refresh().pipe(switchMap((session) => send(session.accessToken)));
  const initial =
    !token && auth.session()
      ? refreshAndSend()
      : send(token).pipe(
          catchError((error: HttpErrorResponse) => {
            if (error.status !== 401 || !auth.session() || !sameAccount()) {
              return throwError(() => error);
            }
            const latest = auth.token();
            if (latest && latest !== token) {
              return send(latest);
            }
            return refreshAndSend();
          }),
        );

  return initial.pipe(
    catchError((error: HttpErrorResponse) => {
      const expiredRefresh =
        [400, 403].includes(error.status) &&
        initialUserId !== null &&
        !auth.session();
      if (
        (error.status === 401 || expiredRefresh) &&
        (sameAccount() || !auth.session())
      ) {
        auth.clearSession();
        void router.navigate(['/login'], {
          queryParams: { returnUrl: router.url },
        });
      }
      return throwError(() => error);
    }),
  );
};
