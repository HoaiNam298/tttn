import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
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
    if (this.form.invalid || this.loading) {
      return;
    }

    this.loading = true;
    this.auth
      .login(
        this.form.controls.phoneNumber.value,
        this.form.controls.password.value,
      )
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
          this.error = 'Thông tin đăng nhập không chính xác.';
          this.loading = false;
        },
      });
  }
}
