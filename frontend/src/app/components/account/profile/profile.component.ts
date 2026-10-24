import { TranslatePipe } from '@ngx-translate/core';
import { MatCardModule } from '@angular/material/card';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  Validators,
} from '@angular/forms';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { finalize } from 'rxjs';
import { ErrorMessageService } from '../../../services/error-message.service';
import { FeedbackService } from '../../../services/feedback.service';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../../../services/auth.service';

@Component({
  selector: 'app-profile',
  imports: [
    TranslatePipe,
    RouterLink,
    MatCardModule,
    MatButtonModule,
    ReactiveFormsModule,
    MatFormFieldModule,
    MatInputModule,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent {
  readonly auth = inject(AuthService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly errors = inject(ErrorMessageService);
  private readonly feedback = inject(FeedbackService);
  saving = false;
  error = '';
  readonly today = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Asia/Ho_Chi_Minh',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
  readonly form = this.fb.nonNullable.group({
    fullName: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(100),
      ],
    ],
    address: [
      '',
      [
        Validators.required,
        Validators.pattern(/\S/),
        Validators.maxLength(255),
      ],
    ],
    dateOfBirth: [
      '',
      (control: AbstractControl) =>
        control.value && control.value >= this.today ? { past: true } : null,
    ],
  });

  constructor() {
    this.reset();
  }

  reset(): void {
    if (this.saving) {
      return;
    }
    const user = this.auth.session()?.user;
    this.form.reset({
      fullName: user?.fullName ?? '',
      address: user?.address ?? '',
      dateOfBirth: user?.dateOfBirth ?? '',
    });
    this.error = '';
  }

  save(): void {
    this.form.markAllAsTouched();
    if (this.saving || this.form.invalid) {
      return;
    }
    const value = this.form.getRawValue();
    this.saving = true;
    this.error = '';
    this.form.disable();
    this.auth
      .updateProfile({
        fullName: value.fullName.trim(),
        address: value.address.trim(),
        dateOfBirth: value.dateOfBirth || null,
      })
      .pipe(
        takeUntilDestroyed(this.destroyRef),
        finalize(() => {
          this.saving = false;
          this.form.enable();
        }),
      )
      .subscribe({
        next: (user) => {
          this.form.reset({
            fullName: user.fullName,
            address: user.address,
            dateOfBirth: user.dateOfBirth ?? '',
          });
          this.feedback.show({
            kind: user.profileSyncPending ? 'warning' : 'success',
            messageKey: user.profileSyncPending
              ? 'account.profileSyncPending'
              : 'account.profileSaved',
          });
        },
        error: (error: unknown) => {
          this.error = this.errors.message(error, 'account.profileSaveFailed');
        },
      });
  }
}
