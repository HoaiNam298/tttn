import {
  HttpBackend,
  HttpClient,
  HttpErrorResponse,
  HttpParams,
} from '@angular/common/http';
import {
  DestroyRef,
  Injectable,
  NgZone,
  computed,
  inject,
  signal,
} from '@angular/core';
import { Router } from '@angular/router';
import {
  Observable,
  TimeoutError,
  catchError,
  defer,
  finalize,
  firstValueFrom,
  from,
  map,
  of,
  shareReplay,
  switchMap,
  throwError,
  timeout,
} from 'rxjs';
import { environment } from '../environments/environment';
import { RegisterPayload } from '../dtos/register.dto';
import { AuthUser } from '../models/auth-user.model';
import { StoredAuthSession } from '../models/stored-auth-session.model';
import { AuthSessionStorage } from './auth-session-storage.service';
import { AuthResponse } from '../responses/auth.response';
import { KeycloakTokenResponse } from '../responses/keycloak-token.response';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = new HttpClient(inject(HttpBackend));
  private readonly router = inject(Router);
  private readonly zone = inject(NgZone);
  private readonly destroyRef = inject(DestroyRef);
  private readonly baseUrl = `${environment.keycloak.url}/realms/${environment.keycloak.realm}/protocol/openid-connect`;
  private readonly storage = inject(AuthSessionStorage);
  private readonly idleTimeout = 30 * 60 * 1000;
  private readonly current = signal<AuthResponse | null>(null);
  private stored: StoredAuthSession | null = null;
  private lastActivity = 0;
  private lastActivityWrite = 0;
  private revision = 0;
  private refreshRequest?: Observable<AuthResponse>;
  readonly userId = computed(() => this.current()?.user.id ?? null);

  constructor() {
    this.restoreTokens();
    this.monitorSession();
  }

  private monitorSession(): void {
    // Only real input extends the idle deadline. HTTP/polling/refresh never does.
    this.zone.runOutsideAngular(() => {
      const onActivity = (event: Event) => {
        if (event.isTrusted) {
          this.recordActivity();
        }
      };
      const onWake = () => this.checkIdle();
      const onStorage = (event: StorageEvent) => this.storageChanged(event);
      const events = [
        'pointerdown',
        'mousemove',
        'keydown',
        'wheel',
        'touchstart',
      ];
      for (const name of events) {
        window.addEventListener(name, onActivity, { passive: true });
      }
      document.addEventListener('visibilitychange', onWake);
      window.addEventListener('focus', onWake);
      window.addEventListener('storage', onStorage);
      const timer = window.setInterval(onWake, 1000);
      this.destroyRef.onDestroy(() => {
        window.clearInterval(timer);
        for (const name of events) {
          window.removeEventListener(name, onActivity);
        }
        document.removeEventListener('visibilitychange', onWake);
        window.removeEventListener('focus', onWake);
        window.removeEventListener('storage', onStorage);
      });
    });
  }

  async initialize(): Promise<void> {
    this.storage.clearLegacyStorage();
    if (!this.stored || this.checkIdle()) {
      return;
    }
    const request =
      Date.now() < this.stored.expiresAt
        ? this.loadProfile(this.stored, this.revision).pipe(
            catchError((error: unknown) =>
              error instanceof HttpErrorResponse && error.status === 401
                ? this.refresh()
                : throwError(() => error),
            ),
          )
        : this.refresh();
    await firstValueFrom(request).catch(() => undefined);
  }

  login(username: string, password: string): Observable<AuthResponse> {
    return defer(() => {
      this.clearSession();
      return this.authenticate(
        new HttpParams()
          .set('grant_type', 'password')
          .set('username', username)
          .set('password', password),
        this.revision,
      );
    });
  }

  register(payload: RegisterPayload): Observable<AuthUser> {
    return this.http
      .post<AuthUser>(`${environment.apiUrl}/auth/register`, payload)
      .pipe(timeout(30000));
  }

  session(): AuthResponse | null {
    return this.current();
  }

  token(): string | null {
    return !this.checkIdle() &&
      this.stored &&
      Date.now() < this.stored.expiresAt
      ? (this.current()?.accessToken ?? null)
      : null;
  }

  isAdmin(): boolean {
    return this.current()?.user.role === 'ADMIN';
  }

  ensureSession(): Observable<boolean> {
    if (!this.stored) {
      this.restoreTokens();
    }
    if (this.checkIdle() || !this.stored) {
      return of(false);
    }
    if (this.current() && this.stored.expiresAt - Date.now() > 30000) {
      return of(true);
    }
    return this.refresh().pipe(
      map(() => true),
      catchError(() => of(false)),
    );
  }

  refresh(): Observable<AuthResponse> {
    if (this.refreshRequest) {
      return this.refreshRequest;
    }
    if (!this.stored || this.checkIdle()) {
      return throwError(() => new Error('No active session'));
    }
    const version = this.revision;
    const lockAbort = new AbortController();
    const originalAccess = this.stored.accessToken;
    const request = defer(() =>
      navigator.locks
        ? from(
            navigator.locks.request(
              'shopapp-keycloak-refresh',
              { signal: lockAbort.signal },
              () =>
                firstValueFrom(this.refreshWithinLock(version, originalAccess)),
            ),
          )
        : this.refreshWithinLock(version, originalAccess),
    ).pipe(
      timeout(25000),
      catchError((error: unknown) => {
        if (
          version === this.revision &&
          !(error instanceof TimeoutError) &&
          (!(error instanceof HttpErrorResponse) ||
            [400, 401, 403].includes(error.status))
        ) {
          this.clearSession();
        }
        return throwError(() => error);
      }),
      finalize(() => {
        lockAbort.abort();
        if (this.refreshRequest === request) {
          this.refreshRequest = undefined;
        }
      }),
      shareReplay({ bufferSize: 1, refCount: false }),
    );
    this.refreshRequest = request;
    return request;
  }

  private refreshWithinLock(
    version: number,
    originalAccess: string,
  ): Observable<AuthResponse> {
    this.syncTokens();
    if (version !== this.revision || !this.stored || this.checkIdle()) {
      return throwError(() => new Error('Session ended'));
    }
    // A different tab may have refreshed while this tab waited for the lock.
    if (
      this.stored.accessToken !== originalAccess &&
      Date.now() < this.stored.expiresAt
    ) {
      return this.loadProfile(this.stored, version);
    }
    return this.authenticate(
      new HttpParams()
        .set('grant_type', 'refresh_token')
        .set('refresh_token', this.stored.refreshToken),
      version,
    );
  }

  logout(): Observable<void> {
    return defer(() => {
      const saved = this.storage.readTokens();
      const refreshToken =
        saved?.id === this.stored?.id
          ? saved?.refreshToken
          : this.stored?.refreshToken;
      this.clearSession();
      return refreshToken
        ? this.http
            .post<void>(
              `${this.baseUrl}/logout`,
              new HttpParams()
                .set('client_id', environment.keycloak.clientId)
                .set('refresh_token', refreshToken),
            )
            .pipe(timeout(10000))
        : of(undefined);
    });
  }

  clearSession(): void {
    const id = this.stored?.id;
    this.resetMemory();
    this.storage.removeSession(id);
  }

  private authenticate(
    body: HttpParams,
    version: number,
  ): Observable<AuthResponse> {
    const refreshing = body.get('grant_type') === 'refresh_token';
    const sessionId = this.stored?.id;
    return this.http
      .post<KeycloakTokenResponse>(
        `${this.baseUrl}/token`,
        body.set('client_id', environment.keycloak.clientId),
      )
      .pipe(
        timeout(10000),
        switchMap((tokens) => {
          if (
            !tokens.access_token ||
            !tokens.refresh_token ||
            !Number.isFinite(tokens.expires_in) ||
            tokens.expires_in <= 0 ||
            version !== this.revision
          ) {
            return throwError(
              () => new Error('Invalid or stale authentication response'),
            );
          }
          const stored: StoredAuthSession = {
            id: sessionId ?? crypto.randomUUID(),
            accessToken: tokens.access_token,
            refreshToken: tokens.refresh_token,
            expiresAt: Date.now() + tokens.expires_in * 1000,
          };
          return this.loadProfile(stored, version, () => {
            if (refreshing) {
              if (
                this.checkIdle() ||
                (this.storage.available &&
                  this.storage.readTokens()?.id !== sessionId)
              ) {
                throw new Error('Session ended during refresh');
              }
            } else {
              this.lastActivity = Date.now();
              this.writeActivity(stored.id);
            }
            this.stored = stored;
            this.storage.writeTokens(stored);
          });
        }),
      );
  }

  private loadProfile(
    stored: StoredAuthSession,
    version: number,
    commit?: () => void,
  ): Observable<AuthResponse> {
    return this.http
      .get<AuthUser>(`${environment.apiUrl}/auth/me`, {
        headers: { Authorization: `Bearer ${stored.accessToken}` },
      })
      .pipe(
        timeout(10000),
        map((user) => {
          if (
            version !== this.revision ||
            !Number.isInteger(user.id) ||
            user.id <= 0 ||
            !['USER', 'ADMIN'].includes(user.role)
          ) {
            throw new Error('Invalid profile or stale session');
          }
          if (!commit && this.checkIdle()) {
            throw new Error('Session idle timeout');
          }
          if (
            !commit &&
            (this.stored?.id !== stored.id ||
              this.stored.accessToken !== stored.accessToken)
          ) {
            throw new Error('Session changed while loading profile');
          }
          commit?.();
          const session: AuthResponse = {
            accessToken: stored.accessToken,
            user,
          };
          this.zone.run(() => this.current.set(session));
          return session;
        }),
      );
  }

  private recordActivity(): void {
    if (!this.stored || this.checkIdle()) {
      return;
    }
    this.lastActivity = Date.now();
    if (this.lastActivity - this.lastActivityWrite >= 5000) {
      this.writeActivity(this.stored.id);
    }
  }

  private checkIdle(): boolean {
    const hadSession = this.stored !== null;
    this.syncTokens();
    if (!this.stored) {
      return hadSession;
    }
    const activity = this.storage.readActivity();
    if (activity?.sessionId === this.stored.id) {
      this.lastActivity = Math.max(this.lastActivity, activity.at);
    }
    if (Date.now() - this.lastActivity < this.idleTimeout) {
      return false;
    }
    this.logout().subscribe({ error: () => undefined });
    this.goToLogin('idle');
    return true;
  }

  private goToLogin(reason?: string): void {
    this.zone.run(() => {
      void this.router.navigate(['/login'], {
        queryParams: reason ? { reason } : {},
      });
    });
  }

  private resetMemory(): void {
    this.revision++;
    this.stored = null;
    this.refreshRequest = undefined;
    this.lastActivity = 0;
    this.lastActivityWrite = 0;
    this.zone.run(() => this.current.set(null));
  }

  private syncTokens(): void {
    const saved = this.storage.readTokens();
    if (!this.storage.available || !this.stored) {
      return;
    }
    if (saved?.id !== this.stored.id) {
      this.resetMemory();
      this.goToLogin();
    } else {
      this.stored = saved;
      const current = this.current();
      if (current && current.accessToken !== saved.accessToken) {
        this.zone.run(() =>
          this.current.set({ ...current, accessToken: saved.accessToken }),
        );
      }
    }
  }

  private storageChanged(event: StorageEvent): void {
    if (this.storage.isSessionEvent(event) && this.stored) {
      const previousAccess = this.stored.accessToken;
      this.syncTokens();
      if (
        this.stored &&
        this.current() &&
        this.stored.accessToken !== previousAccess
      ) {
        this.loadProfile(this.stored, this.revision).subscribe({
          error: () => undefined,
        });
      }
    }
  }

  private restoreTokens(): void {
    this.stored = this.storage.readTokens();
    const activity = this.storage.readActivity();
    this.lastActivity =
      activity?.sessionId === this.stored?.id ? (activity?.at ?? 0) : 0;
  }

  private writeActivity(id: string): void {
    this.lastActivityWrite = this.lastActivity;
    this.storage.writeActivity(id, this.lastActivity);
  }
}
