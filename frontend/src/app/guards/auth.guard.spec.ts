import { TestBed } from '@angular/core/testing';
import {
  ActivatedRouteSnapshot,
  RouterStateSnapshot,
  UrlTree,
  provideRouter,
} from '@angular/router';
import { Observable, Subject, firstValueFrom } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { authGuard } from './auth.guard';

describe('Authentication guard session restoration', () => {
  it('waits for refresh before permitting or rejecting a protected route', async () => {
    const restored = new Subject<boolean>();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { ensureSession: () => restored } },
      ],
    });
    const pending = TestBed.runInInjectionContext(() =>
      authGuard(
        {} as ActivatedRouteSnapshot,
        { url: '/orders' } as RouterStateSnapshot,
      ),
    ) as Observable<boolean | UrlTree>;
    let completed = false;
    const result = firstValueFrom(pending).then((value) => {
      completed = true;
      return value;
    });
    expect(completed).toBe(false);
    restored.next(true);
    expect(await result).toBe(true);
  });
});
