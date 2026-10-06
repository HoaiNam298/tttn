import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-profile',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './profile.component.html',
})
export class ProfileComponent {
  readonly auth = inject(AuthService);
}
