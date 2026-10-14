import {
  Component,
  inject,
  ChangeDetectionStrategy,
  DestroyRef,
  OnInit,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatProgressBarModule } from '@angular/material/progress-bar';

@Component({
  selector: 'app-login',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  showPassword = false;
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
  readonly registered =
    this.route.snapshot.queryParamMap.get('registered') === '1';
  readonly idleExpired =
    this.route.snapshot.queryParamMap.get('reason') === 'idle';

  loading = false;
  error = '';
  ngOnInit(): void {
    if (this.auth.session()) {
      void this.router.navigateByUrl(
        this.destination(this.auth.isAdmin() ? 'ADMIN' : 'USER'),
      );
    }
  }
  form = this.fb.nonNullable.group({
    phoneNumber: ['', Validators.required],
    password: ['', Validators.required],
  });
  submit(): void {
    if (this.loading) {
      return;
    }
    this.form.markAllAsTouched();
    if (this.form.invalid) {
      return;
    }

    this.loading = true;
    this.form.disable({ emitEvent: false });
    this.error = '';
    this.auth
      .login(
        this.form.controls.phoneNumber.value,
        this.form.controls.password.value,
      )
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (value) => {
          void this.router.navigateByUrl(this.destination(value.user.role));
        },
        error: () => {
          this.form.enable({ emitEvent: false });
          this.error = 'Thông tin đăng nhập không chính xác.';
          this.loading = false;
        },
      });
  }

  private destination(role: string): string {
    const path = this.returnUrl;
    if (path?.startsWith('/') && !path.startsWith('//')) {
      try {
        const url = new URL(path, window.location.origin);
        if (url.origin === window.location.origin) {
          return `${url.pathname}${url.search}`;
        }
      } catch {
        // Malformed return URLs use the normal account destination.
      }
    }
    return role === 'ADMIN' ? '/admin' : '/products';
  }
}
