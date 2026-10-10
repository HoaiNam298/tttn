import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { DestroyRef } from '@angular/core';
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
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  showPassword = false;
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');

  loading = false;
  error = '';
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
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
          const destination =
            (returnUrl?.startsWith('/') && !returnUrl.startsWith('//')
              ? returnUrl
              : null) ?? (value.user.role === 'ADMIN' ? '/admin' : '/products');
          void this.router.navigateByUrl(destination);
        },
        error: () => {
          this.form.enable({ emitEvent: false });
          this.error = 'Thông tin đăng nhập không chính xác.';
          this.loading = false;
        },
      });
  }
}
