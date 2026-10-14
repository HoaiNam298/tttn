import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  provideRouter,
} from '@angular/router';
import { AuthService } from '../services/auth.service';
import { adminGuard } from './admin.guard';
import { firstValueFrom, Observable, of } from 'rxjs';
import { UrlTree } from '@angular/router';

describe('Admin route authorization', () => {
  const auth = { ensureSession: vi.fn(), isAdmin: vi.fn() };
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
  });
  const check = () =>
    TestBed.runInInjectionContext(() =>
      adminGuard(
        {} as ActivatedRouteSnapshot,
        { url: '/admin/dashboard' } as RouterStateSnapshot,
      ),
    );

  it('redirects an unrecoverable admin session to login', async () => {
    auth.ensureSession.mockReturnValue(of(false));
    auth.isAdmin.mockReturnValue(true);
    expect(
      String(await firstValueFrom(check() as Observable<boolean | UrlTree>)),
    ).toBe('/login?returnUrl=%2Fadmin%2Fdashboard');
  });
  it('rejects direct admin navigation for a customer', async () => {
    auth.ensureSession.mockReturnValue(of(true));
    auth.isAdmin.mockReturnValue(false);
    expect(
      String(await firstValueFrom(check() as Observable<boolean | UrlTree>)),
    ).toBe('/products');
  });
  it('permits authenticated administrators after session restoration', async () => {
    auth.ensureSession.mockReturnValue(of(true));
    auth.isAdmin.mockReturnValue(true);
    expect(await firstValueFrom(check() as Observable<boolean | UrlTree>)).toBe(
      true,
    );
  });
});
