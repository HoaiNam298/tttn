import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { of, throwError } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { AccountComponent } from './account.component';

describe('Account logout', () => {
  it.each([false, true])(
    'returns to login after local logout even when remote revocation fails=%s',
    (failed) => {
      const navigate = vi.fn().mockResolvedValue(true);
      const open = vi.fn();
      TestBed.configureTestingModule({
        providers: [
          {
            provide: AuthService,
            useValue: {
              logout: () =>
                failed ? throwError(() => new Error('Offline')) : of(undefined),
            },
          },
          { provide: Router, useValue: { navigate } },
          { provide: MatSnackBar, useValue: { open } },
        ],
      });
      const component = TestBed.runInInjectionContext(
        () => new AccountComponent(),
      );
      component.logout();
      expect(navigate).toHaveBeenCalledWith(['/login']);
      expect(open).toHaveBeenCalledTimes(failed ? 1 : 0);
    },
  );
});
