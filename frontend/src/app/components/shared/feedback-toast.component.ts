import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { RouterLink } from '@angular/router';
import {
  MAT_SNACK_BAR_DATA,
  MatSnackBarRef,
} from '@angular/material/snack-bar';
import { FeedbackMessage } from '../../models/feedback.model';

@Component({
  selector: 'app-feedback-toast',
  imports: [TranslatePipe, RouterLink],
  templateUrl: './feedback-toast.component.html',
  styleUrl: './feedback-toast.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class FeedbackToastComponent {
  readonly data = inject<FeedbackMessage>(MAT_SNACK_BAR_DATA);
  private readonly ref = inject(MatSnackBarRef<FeedbackToastComponent>);
  readonly icon = { success: '✓', info: 'i', warning: '!', error: '×' }[
    this.data.kind
  ];

  close(): void {
    this.ref.dismiss();
  }
}
