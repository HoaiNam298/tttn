import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { MatSnackBar } from '@angular/material/snack-bar';

@Component({
  selector: 'app-account',
  imports: [
    TranslatePipe,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatButtonModule,
    MatCardModule,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './account.component.html',
  styleUrl: './account.component.scss',
})
export class AccountComponent {
  private readonly translate = inject(TranslateService);
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly snack = inject(MatSnackBar);
  loggingOut = false;

  logout(): void {
    if (this.loggingOut) {
      return;
    }
    this.loggingOut = true;
    this.auth.logout().subscribe({
      next: () => {
        void this.router.navigate(['/login']);
      },
      error: () => {
        this.loggingOut = false;
        void this.router.navigate(['/login']);
        this.snack.open(
          this.translate.instant(
            'account.theBrowserSessionWasClearedButTheServerSession',
          ),
          this.translate.instant('account.close'),
          { duration: 5000 },
        );
      },
    });
  }
}
