import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './register.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class RegisterComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  readonly returnUrl = this.route.snapshot.queryParamMap.get('returnUrl');
  loading = false;
  error = '';
  form = this.fb.nonNullable.group({
    fullName: ['', [Validators.required, Validators.maxLength(100)]],
    phoneNumber: [
      '',
      [Validators.required, Validators.pattern(/^[0-9]{9,15}$/)],
    ],
    address: ['', [Validators.required, Validators.maxLength(255)]],
    password: [
      '',
      [Validators.required, Validators.minLength(8), Validators.maxLength(72)],
    ],
    confirmPassword: ['', Validators.required],
  });

  submit(): void {
    if (this.loading) {
      return;
    }
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    if (this.form.invalid || value.password !== value.confirmPassword) {
      this.error =
        'Kiểm tra thông tin và xác nhận mật khẩu (tối thiểu 8 ký tự).';
      return;
    }
    this.loading = true;
    this.error = '';
    this.auth
      .register({
        fullName: value.fullName.trim(),
        phoneNumber: value.phoneNumber,
        address: value.address.trim(),
        password: value.password,
      })
      .subscribe({
        next: () => {
          const destination =
            this.returnUrl?.startsWith('/') && !this.returnUrl.startsWith('//')
              ? this.returnUrl
              : '/products';
          void this.router.navigateByUrl(destination);
        },
        error: (response) => {
          this.loading = false;
          this.error =
            response.status === 409
              ? 'Số điện thoại đã được đăng ký.'
              : 'Không thể đăng ký. Vui lòng kiểm tra dữ liệu và thử lại.';
        },
      });
  }
}
