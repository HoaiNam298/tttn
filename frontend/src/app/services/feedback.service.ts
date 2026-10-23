import { Injectable, inject } from '@angular/core';
import { MatSnackBar } from '@angular/material/snack-bar';
import { FeedbackMessage } from '../models/feedback.model';
import { FeedbackToastComponent } from '../components/shared/feedback-toast.component';

@Injectable({ providedIn: 'root' })
export class FeedbackService {
  private readonly snack = inject(MatSnackBar);

  show(message: FeedbackMessage): void {
    // Material replaces the current snackbar rather than stacking retry notices.
    this.snack.openFromComponent(FeedbackToastComponent, {
      data: message,
      duration:
        message.kind === 'success' || message.kind === 'info' ? 6000 : 0,
      horizontalPosition: 'end',
      verticalPosition: 'bottom',
      panelClass: 'shop-feedback',
      // The component owns its translated live region, including locale changes.
      politeness: 'off',
    });
  }
}
