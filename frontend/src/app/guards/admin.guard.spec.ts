import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  Router,
  RouterStateSnapshot,
  provideRouter,
} from '@angular/router';
import { AuthService } from '../services/auth.service';
import { adminGuard } from './admin.guard';

describe('Admin route authorization', () => {
  const auth = { token: vi.fn(), isAdmin: vi.fn() };
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

  it('redirects an expired admin session to login', () => {
    auth.token.mockReturnValue(null);
    auth.isAdmin.mockReturnValue(true);
    expect(String(check())).toBe('/login?returnUrl=%2Fadmin%2Fdashboard');
  });
  it('rejects direct admin navigation for a customer', () => {
    auth.token.mockReturnValue('valid');
    auth.isAdmin.mockReturnValue(false);
    expect(String(check())).toBe('/products');
  });
  it('permits authenticated administrators', () => {
    auth.token.mockReturnValue('valid');
    auth.isAdmin.mockReturnValue(true);
    expect(check()).toBe(true);
  });
});
