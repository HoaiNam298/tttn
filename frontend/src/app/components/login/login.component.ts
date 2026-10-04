import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './login.component.html',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  loading = false;
  error = '';
  form = this.fb.nonNullable.group({
    phoneNumber: ['', Validators.required],
    password: ['', Validators.required],
  });
  submit(): void {
    if (this.form.invalid) {
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
          if (value.user.role !== 'ADMIN') {
            this.auth.logout().subscribe();
            this.error = 'Tài khoản không có quyền Admin.';
            this.loading = false;
            return;
          }

          void this.router.navigate(['/admin']);
        },
        error: () => {
          this.error = 'Thông tin đăng nhập không chính xác.';
          this.loading = false;
        },
      });
  }
}
