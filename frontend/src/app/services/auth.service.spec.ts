import {
  HttpClient,
  provideHttpClient,
  withInterceptors,
} from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { firstValueFrom } from 'rxjs';
import { environment } from '../environments/environment';
import { authInterceptor } from '../interceptors/auth.interceptor';
import { AuthService } from './auth.service';
import { StoredAuthSession } from '../models/stored-auth-session.model';
import { AuthResponse } from '../responses/auth.response';

describe('Direct Keycloak authentication', () => {
  const endpoint = `${environment.keycloak.url}/realms/${environment.keycloak.realm}/protocol/openid-connect`;
  const user = {
    id: 42,
    fullName: 'Customer',
    phoneNumber: '0900000000',
    address: 'HCM',
    role: 'USER',
  };
  const tokens = {
    access_token: 'access-token',
    refresh_token: 'refresh-token',
    expires_in: 300,
    token_type: 'Bearer',
  };
  const key = 'shopapp_keycloak_session_v1';
  const activityKey = 'shopapp_keycloak_activity_v1';
  const saved = () =>
    JSON.parse(localStorage.getItem(key) ?? 'null') as StoredAuthSession | null;
  let userActivity: EventListener;
  let auth: AuthService;
  let requests: HttpTestingController;

  async function signedIn(): Promise<void> {
    const result = firstValueFrom(auth.login('0900000000', 'password123'));
    requests.expectOne(endpoint + '/token').flush(tokens);
    requests.expectOne('/api/v1/auth/me').flush(user);
    await result;
  }

  beforeEach(() => {
    vi.useFakeTimers();
    sessionStorage.clear();
    localStorage.clear();
    const addListener = window.addEventListener.bind(window);
    vi.spyOn(window, 'addEventListener').mockImplementation(
      (name, listener, options) => {
        if (name === 'pointerdown') {
          userActivity = listener as EventListener;
        }
        addListener(name, listener, options);
      },
    );
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    requests = TestBed.inject(HttpTestingController);
    auth = TestBed.inject(AuthService);
    vi.spyOn(TestBed.inject(Router), 'navigate').mockResolvedValue(true);
  });

  afterEach(() => {
    requests.verify();
    TestBed.resetTestingModule();
    vi.useRealTimers();
    vi.restoreAllMocks();
    vi.unstubAllGlobals();
    sessionStorage.clear();
    localStorage.clear();
  });

  it('removes obsolete sessionStorage tokens without migrating an unknown idle deadline', async () => {
    sessionStorage.setItem('shopapp_keycloak_refresh_v1', 'old-refresh');
    localStorage.setItem('shopapp_auth', 'old-local-auth');
    await auth.initialize();
    expect(sessionStorage.length).toBe(0);
    expect(localStorage.getItem('shopapp_auth')).toBeNull();
    requests.expectNone(endpoint + '/token');
  });

  it('reopens the app with persisted tokens but loads profile/roles from the backend', async () => {
    await signedIn();
    const reloaded = TestBed.runInInjectionContext(() => new AuthService());
    const initialized = reloaded.initialize();
    const profile = requests.expectOne('/api/v1/auth/me');
    expect(profile.request.headers.get('Authorization')).toBe(
      'Bearer access-token',
    );
    profile.flush({ ...user, role: 'ADMIN' });
    await initialized;
    expect(reloaded.isAdmin()).toBe(true);
    requests.expectNone(endpoint + '/token');
  });

  it('logs out after 30 minutes even while background refresh keeps tokens valid', async () => {
    await signedIn();
    const started = localStorage.getItem(activityKey);
    for (let index = 0; index < 7; index++) {
      vi.advanceTimersByTime(4 * 60 * 1000);
      const refresh = firstValueFrom(auth.refresh());
      requests.expectOne(endpoint + '/token').flush(tokens);
      requests.expectOne('/api/v1/auth/me').flush(user);
      await refresh;
    }
    expect(localStorage.getItem(activityKey)).toBe(started);
    vi.advanceTimersByTime(2 * 60 * 1000);
    requests
      .expectOne(endpoint + '/logout')
      .flush(null, { status: 204, statusText: 'No Content' });
    expect(auth.session()).toBeNull();
    expect(localStorage.getItem(key)).toBeNull();
    expect(localStorage.getItem(activityKey)).toBeNull();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { reason: 'idle' },
    });
  });

  it('extends the deadline on real input but not on synthetic input', async () => {
    await signedIn();
    const started = localStorage.getItem(activityKey);
    vi.advanceTimersByTime(20 * 60 * 1000);
    window.dispatchEvent(new Event('pointerdown'));
    expect(localStorage.getItem(activityKey)).toBe(started);
    userActivity({ isTrusted: true } as Event);
    vi.advanceTimersByTime(20 * 60 * 1000);
    expect(auth.userId()).toBe(42);
    requests.expectNone(endpoint + '/logout');
    vi.advanceTimersByTime(10 * 60 * 1000);
    requests
      .expectOne(endpoint + '/logout')
      .flush(null, { status: 204, statusText: 'No Content' });
    expect(auth.userId()).toBeNull();
  });

  it('uses the activity deadline shared by other tabs', async () => {
    await signedIn();
    vi.advanceTimersByTime(20 * 60 * 1000);
    localStorage.setItem(
      activityKey,
      JSON.stringify({ sessionId: saved()?.id, at: Date.now() }),
    );
    vi.advanceTimersByTime(20 * 60 * 1000);
    expect(auth.userId()).toBe(42);
    requests.expectNone(endpoint + '/logout');
    vi.advanceTimersByTime(10 * 60 * 1000);
    requests
      .expectOne(endpoint + '/logout')
      .flush(null, { status: 204, statusText: 'No Content' });
    expect(auth.userId()).toBeNull();
  });

  it('does not refresh an idle-expired session when reopening the app', async () => {
    localStorage.setItem(
      key,
      JSON.stringify({
        id: 'old-session',
        accessToken: 'old-access',
        refreshToken: 'old-refresh',
        expiresAt: Date.now() + 60000,
      }),
    );
    localStorage.setItem(
      activityKey,
      JSON.stringify({
        sessionId: 'old-session',
        at: Date.now() - 30 * 60 * 1000,
      }),
    );
    const reloaded = TestBed.runInInjectionContext(() => new AuthService());
    await reloaded.initialize();
    requests
      .expectOne(endpoint + '/logout')
      .flush(null, { status: 204, statusText: 'No Content' });
    requests.expectNone(endpoint + '/token');
    requests.expectNone('/api/v1/auth/me');
    expect(reloaded.session()).toBeNull();
    expect(localStorage.getItem(key)).toBeNull();
  });

  it('propagates logout from another tab and blocks its pending refresh from restoring tokens', async () => {
    await signedIn();
    const result = firstValueFrom(auth.refresh()).catch((error) => error);
    const pending = requests.expectOne(endpoint + '/token');
    localStorage.removeItem(key);
    localStorage.removeItem(activityKey);
    window.dispatchEvent(new StorageEvent('storage', { key, newValue: null }));
    pending.flush(tokens);
    expect(await result).toBeInstanceOf(Error);
    requests.expectNone('/api/v1/auth/me');
    expect(auth.userId()).toBeNull();
    expect(localStorage.getItem(key)).toBeNull();
    expect(TestBed.inject(Router).navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: {},
    });
  });

  it('does not clear or revoke a newer account logged in by another tab', async () => {
    await signedIn();
    const result = firstValueFrom(auth.refresh()).catch((error) => error);
    const pending = requests.expectOne(endpoint + '/token');
    const newer = {
      id: 'new-session',
      accessToken: 'new-access',
      refreshToken: 'new-refresh',
      expiresAt: Date.now() + 60000,
    };
    localStorage.setItem(key, JSON.stringify(newer));
    localStorage.setItem(
      activityKey,
      JSON.stringify({ sessionId: newer.id, at: Date.now() }),
    );
    window.dispatchEvent(
      new StorageEvent('storage', { key, newValue: JSON.stringify(newer) }),
    );
    pending.flush(tokens);
    await result;
    expect(saved()?.id).toBe('new-session');
    expect(auth.session()).toBeNull();
    requests.expectNone(endpoint + '/logout');
  });

  it('serializes refresh across tabs and reuses the token refreshed by the first tab', async () => {
    await signedIn();
    const other = TestBed.runInInjectionContext(() => new AuthService());
    let queue: Promise<unknown> = Promise.resolve();
    const lock = vi.fn(
      (
        _name: string,
        _options: LockOptions,
        operation: () => Promise<AuthResponse>,
      ) => {
        const result = queue.then(operation);
        queue = result.catch(() => undefined);
        return result;
      },
    );
    vi.stubGlobal('navigator', { locks: { request: lock } });
    const first = firstValueFrom(auth.refresh());
    const second = firstValueFrom(other.refresh());
    await Promise.resolve();
    requests
      .expectOne(endpoint + '/token')
      .flush({ ...tokens, access_token: 'rotated' });
    requests.expectOne('/api/v1/auth/me').flush(user);
    await first;
    for (let index = 0; index < 5; index++) {
      await Promise.resolve();
    }
    requests.expectOne('/api/v1/auth/me').flush(user);
    expect((await second).accessToken).toBe('rotated');
    expect(lock).toHaveBeenCalledTimes(2);
    requests.expectNone(endpoint + '/token');
  });

  it('posts credentials without a client secret, then resolves the verified ShopApp profile', async () => {
    const result = firstValueFrom(auth.login('0900000000', 'password123'));
    const login = requests.expectOne(endpoint + '/token');
    expect(login.request.body.get('grant_type')).toBe('password');
    expect(login.request.body.get('username')).toBe('0900000000');
    expect(login.request.body.get('password')).toBe('password123');
    expect(login.request.body.get('client_id')).toBe('shopapp-web');
    expect(login.request.body.has('client_secret')).toBe(false);
    login.flush(tokens);
    const profile = requests.expectOne('/api/v1/auth/me');
    expect(auth.session()).toBeNull();
    expect(profile.request.headers.get('Authorization')).toBe(
      'Bearer access-token',
    );
    profile.flush(user);
    expect((await result).user.id).toBe(42);
    expect(auth.userId()).toBe(42);
    expect(auth.isAdmin()).toBe(false);
    expect(saved()?.refreshToken).toBe('refresh-token');
    expect(saved()?.accessToken).toBe('access-token');
    expect(JSON.stringify(localStorage)).not.toContain('password123');
    expect(sessionStorage.length).toBe(0);
  });

  it('does not establish a session when credentials fail or the profile is missing', async () => {
    const failure = firstValueFrom(auth.login('0900000000', 'wrong')).catch(
      (error) => error,
    );
    requests
      .expectOne(endpoint + '/token')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    expect((await failure).status).toBe(401);
    expect(auth.session()).toBeNull();
    const unlinked = firstValueFrom(
      auth.login('0900000000', 'password123'),
    ).catch((error) => error);
    requests.expectOne(endpoint + '/token').flush(tokens);
    requests
      .expectOne('/api/v1/auth/me')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    expect((await unlinked).status).toBe(401);
    expect(localStorage.getItem(key)).toBeNull();
  });

  it('sends one refresh for simultaneous callers and reloads token roles from the backend', async () => {
    await signedIn();
    const first = firstValueFrom(auth.refresh());
    const second = firstValueFrom(auth.refresh());
    const refresh = requests.expectOne(endpoint + '/token');
    expect(refresh.request.body.get('grant_type')).toBe('refresh_token');
    expect(refresh.request.body.get('refresh_token')).toBe('refresh-token');
    expect(refresh.request.body.has('client_secret')).toBe(false);
    refresh.flush({
      ...tokens,
      access_token: 'rotated',
      refresh_token: 'rotated-refresh',
    });
    requests.expectOne('/api/v1/auth/me').flush({ ...user, role: 'ADMIN' });
    expect((await first).accessToken).toBe('rotated');
    expect((await second).accessToken).toBe('rotated');
    expect(auth.isAdmin()).toBe(true);
    expect(saved()?.refreshToken).toBe('rotated-refresh');
  });

  it('restores a tab through refresh on reload instead of trusting stored profile/roles', async () => {
    localStorage.setItem(
      key,
      JSON.stringify({
        id: 'saved-session',
        accessToken: 'expired-access',
        refreshToken: 'saved-refresh',
        expiresAt: Date.now() - 1,
      }),
    );
    localStorage.setItem(
      activityKey,
      JSON.stringify({ sessionId: 'saved-session', at: Date.now() }),
    );
    const reloaded = TestBed.runInInjectionContext(() => new AuthService());
    const initialized = reloaded.initialize();
    const refresh = requests.expectOne(endpoint + '/token');
    expect(refresh.request.body.get('refresh_token')).toBe('saved-refresh');
    refresh.flush(tokens);
    requests.expectOne('/api/v1/auth/me').flush(user);
    await initialized;
    expect(reloaded.userId()).toBe(42);
  });

  it('preserves the stored session when a refresh request times out and allows retry', async () => {
    await signedIn();
    const result = firstValueFrom(auth.refresh()).catch((error) => error);
    const pending = requests.expectOne(endpoint + '/token');
    vi.advanceTimersByTime(10000);
    expect((await result).name).toBe('TimeoutError');
    expect(pending.cancelled).toBe(true);
    expect(saved()?.refreshToken).toBe('refresh-token');
    const retried = firstValueFrom(auth.refresh());
    requests.expectOne(endpoint + '/token').flush(tokens);
    requests.expectOne('/api/v1/auth/me').flush(user);
    expect((await retried).user.id).toBe(42);
  });

  it('bounds waiting for another tab refresh lock without deleting valid stored tokens', async () => {
    await signedIn();
    let signal: AbortSignal | undefined;
    vi.stubGlobal('navigator', {
      locks: {
        request: (_name: string, options: LockOptions) => {
          signal = options.signal;
          return new Promise((_resolve, reject) => {
            signal?.addEventListener('abort', () =>
              reject(new DOMException('Aborted', 'AbortError')),
            );
          });
        },
      },
    });
    const result = firstValueFrom(auth.refresh()).catch((error) => error);
    vi.advanceTimersByTime(25000);
    expect((await result).name).toBe('TimeoutError');
    expect(signal?.aborted).toBe(true);
    expect(saved()?.refreshToken).toBe('refresh-token');
    requests.expectNone(endpoint + '/token');
  });

  it('retains the refresh token on a temporary outage and can recover on retry', async () => {
    await signedIn();
    const failed = firstValueFrom(auth.refresh()).catch((error) => error);
    requests
      .expectOne(endpoint + '/token')
      .flush({}, { status: 503, statusText: 'Unavailable' });
    expect((await failed).status).toBe(503);
    expect(saved()?.refreshToken).toBe('refresh-token');
    const retried = firstValueFrom(auth.refresh());
    requests.expectOne(endpoint + '/token').flush(tokens);
    requests.expectOne('/api/v1/auth/me').flush(user);
    expect((await retried).user.id).toBe(42);
  });

  it('clears the session after a rejected refresh', async () => {
    await signedIn();
    const result = firstValueFrom(auth.refresh()).catch((error) => error);
    requests
      .expectOne(endpoint + '/token')
      .flush({}, { status: 400, statusText: 'Bad Request' });
    expect((await result).status).toBe(400);
    expect(auth.session()).toBeNull();
    expect(auth.userId()).toBeNull();
    expect(localStorage.getItem(key)).toBeNull();
  });

  it('revokes the Keycloak session on logout and does not restore a stale refresh response', async () => {
    await signedIn();
    const refreshed = firstValueFrom(auth.refresh()).catch((error) => error);
    const pending = requests.expectOne(endpoint + '/token');
    const logout = firstValueFrom(auth.logout());
    const revoke = requests.expectOne(endpoint + '/logout');
    expect(revoke.request.body.get('refresh_token')).toBe('refresh-token');
    expect(revoke.request.body.has('client_secret')).toBe(false);
    revoke.flush(null, { status: 204, statusText: 'No Content' });
    await logout;
    pending.flush(tokens);
    requests.expectNone('/api/v1/auth/me');
    expect(await refreshed).toBeInstanceOf(Error);
    expect(auth.session()).toBeNull();
    expect(localStorage.getItem(key)).toBeNull();
  });

  it('returns to the login form when an API 401 is followed by an expired refresh token', async () => {
    await signedIn();
    const navigate = vi
      .spyOn(TestBed.inject(Router), 'navigate')
      .mockResolvedValue(true);
    const result = firstValueFrom(
      TestBed.inject(HttpClient).get('/api/v1/orders'),
    ).catch((error) => error);
    requests
      .expectOne('/api/v1/orders')
      .flush({}, { status: 401, statusText: 'Unauthorized' });
    requests
      .expectOne(endpoint + '/token')
      .flush({}, { status: 400, statusText: 'Bad Request' });
    expect((await result).status).toBe(400);
    expect(auth.session()).toBeNull();
    expect(navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/' },
    });
  });

  it('attaches tokens only to ShopApp and retries an API 401 once with a refreshed token', async () => {
    await signedIn();
    const http = TestBed.inject(HttpClient);
    const own = firstValueFrom(http.get('/api/v1/orders'));
    const initial = requests.expectOne('/api/v1/orders');
    expect(initial.request.headers.get('Authorization')).toBe(
      'Bearer access-token',
    );
    initial.flush({}, { status: 401, statusText: 'Unauthorized' });
    requests
      .expectOne(endpoint + '/token')
      .flush({ ...tokens, access_token: 'rotated' });
    requests.expectOne('/api/v1/auth/me').flush(user);
    const retry = requests.expectOne('/api/v1/orders');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer rotated');
    retry.flush([]);
    await own;
    const external = firstValueFrom(http.get('https://example.invalid/data'));
    const request = requests.expectOne('https://example.invalid/data');
    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({});
    await external;
    requests.expectNone('/api/v1/auth/refresh');
  });
});
