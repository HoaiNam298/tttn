import { Component, ChangeDetectionStrategy } from '@angular/core';
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
} from '@angular/router';
import { CartService } from './services/cart.service';
import { AuthService } from './services/auth.service';
import { FormBuilder, ReactiveFormsModule } from '@angular/forms';
import { inject } from '@angular/core';
import { NotificationService } from './services/notification.service';

@Component({
  selector: 'app-root',
  imports: [RouterLink, RouterLinkActive, RouterOutlet, ReactiveFormsModule],
  templateUrl: './app.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
  styleUrl: './app.component.scss',
})
export class AppComponent {
  readonly notifications = inject(NotificationService);
  readonly auth = inject(AuthService);
  readonly cart = inject(CartService);
  readonly router = inject(Router);
  readonly searchForm = inject(FormBuilder).nonNullable.group({
    keyword: [''],
  });

  search(): void {
    void this.router.navigate(['/products'], {
      queryParams: {
        keyword: this.searchForm.getRawValue().keyword.trim() || null,
      },
    });
  }
}
