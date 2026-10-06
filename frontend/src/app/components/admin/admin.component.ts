import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [RouterLink, RouterLinkActive, RouterOutlet],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  logout(): void {
    this.auth.logout().subscribe(() => {
      void this.router.navigate(['/login']);
    });
  }
}
