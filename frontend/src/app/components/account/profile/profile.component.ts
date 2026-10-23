import { TranslatePipe } from '@ngx-translate/core';
import { MatCardModule } from '@angular/material/card';
import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-profile',
  imports: [TranslatePipe, RouterLink, MatCardModule, MatButtonModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent {
  readonly auth = inject(AuthService);
}
