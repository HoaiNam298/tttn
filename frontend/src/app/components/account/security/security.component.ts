import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../../services/auth.service';
import { ErrorMessageService } from '../../../services/error-message.service';
import { FeedbackService } from '../../../services/feedback.service';

@Component({
  selector: 'app-security',
  imports: [
    TranslatePipe,
    ReactiveFormsModule,
    MatCardModule,
    MatButtonModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  templateUrl: './security.component.html',
  styleUrl: './security.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class SecurityComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly errors = inject(ErrorMessageService);
  private readonly feedback = inject(FeedbackService);
  private readonly destroyRef = inject(DestroyRef);
  saving = false;
  showPassword = false;
  error = '';
  readonly form = this.fb.nonNullable.group(
    {
      currentPassword: ['', [Validators.required, Validators.maxLength(1024)]],
      newPassword: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(72),
        ],
      ],
      confirmPassword: ['', Validators.required],
    },
    {
      validators: (control) => {
        const { currentPassword, newPassword, confirmPassword } =
          control.getRawValue();
        if (newPassword && newPassword === currentPassword) {
          return { unchanged: true };
        }
        return newPassword !== confirmPassword ? { mismatch: true } : null;
      },
    },
  );

  constructor() {
    this.destroyRef.onDestroy(() => this.form.reset());
  }

  submit(): void {
    this.form.markAllAsTouched();
    if (this.saving || this.form.invalid) {
      return;
    }
    const { currentPassword, newPassword } = this.form.getRawValue();
    this.saving = true;
    this.error = '';
    this.form.disable();
    this.auth
      .changePassword({ currentPassword, newPassword })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.saving = false;
          this.form.enable();
        }),
      )
      .subscribe({
        next: () => {
          this.form.reset();
          this.feedback.show({
            kind: 'success',
            messageKey: 'account.passwordChanged',
          });
          void this.router.navigate(['/login']);
        },
        error: (error: unknown) => {
          this.error = this.errors.message(
            error,
            'account.passwordChangeFailed',
          );
          this.form.reset();
          if (!this.auth.session()) {
            const code =
              error instanceof HttpErrorResponse ? error.error?.code : '';
            this.feedback.show({
              kind: 'warning',
              messageKey:
                code === 'PASSWORD_CHANGED_LOGOUT_PENDING'
                  ? 'account.passwordLogoutPending'
                  : code === 'PASSWORD_POLICY'
                    ? 'account.passwordPolicySignIn'
                    : 'account.passwordOutcomeUnknown',
            });
            void this.router.navigate(['/login']);
          }
        },
      });
  }
}
