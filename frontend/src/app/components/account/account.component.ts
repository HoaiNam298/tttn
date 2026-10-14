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
          'Đã xóa phiên trên trình duyệt nhưng chưa thể xác nhận kết thúc phiên trên máy chủ.',
          'Đóng',
          { duration: 5000 },
        );
      },
    });
  }
}
