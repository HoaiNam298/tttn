import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
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
  selector: 'app-register',
  imports: [
    ReactiveFormsModule,
    RouterLink,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatProgressBarModule,
  ],
  templateUrl: './register.component.html',
  styleUrl: './register.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class RegisterComponent {
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
    this.form.disable({ emitEvent: false });
    this.error = '';
    this.auth
      .register({
        fullName: value.fullName.trim(),
        phoneNumber: value.phoneNumber,
        address: value.address.trim(),
        password: value.password,
      })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          const destination =
            this.returnUrl?.startsWith('/') && !this.returnUrl.startsWith('//')
              ? this.returnUrl
              : '/products';
          void this.router.navigateByUrl(destination);
        },
        error: (response) => {
          this.form.enable({ emitEvent: false });
          this.loading = false;
          this.error =
            response.status === 409
              ? 'Số điện thoại đã được đăng ký.'
              : 'Không thể đăng ký. Vui lòng kiểm tra dữ liệu và thử lại.';
        },
      });
  }
}
