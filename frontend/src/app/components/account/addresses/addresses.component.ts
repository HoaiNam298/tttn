import { ErrorMessageService } from '../../../services/error-message.service';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import {
  DestroyRef,
  ChangeDetectionStrategy,
  Component,
  TemplateRef,
  OnInit,
  ViewChild,
  inject,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ConfirmationDialogComponent } from '../../shared/confirmation-dialog.component';
import {
  MatDialog,
  MatDialogRef,
  MatDialogModule,
} from '@angular/material/dialog';

import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';

import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Address } from '../../../models/address.model';
import { AddressService } from '../../../services/address.service';

@Component({
  selector: 'app-addresses',
  imports: [
    TranslatePipe,
    ReactiveFormsModule,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatCheckboxModule,
    MatProgressBarModule,
    MatDialogModule,
  ],
  templateUrl: './addresses.component.html',
  styleUrl: './addresses.component.scss',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class AddressesComponent implements OnInit {
  private readonly errorMessages = inject(ErrorMessageService);
  private readonly translate = inject(TranslateService);
  private confirmationOpen = false;

  private readonly service = inject(AddressService);
  private readonly fb = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  @ViewChild('editor') private editor!: TemplateRef<unknown>;
  private readonly dialog = inject(MatDialog);
  private editorDialog?: MatDialogRef<unknown>;
  addresses: Address[] = [];
  loading = false;
  saving = false;
  editingId?: number;
  error = '';
  formError = '';
  form = this.fb.nonNullable.group({
    recipientName: ['', [Validators.required, Validators.maxLength(100)]],
    phoneNumber: [
      '',
      [Validators.required, Validators.pattern(/^[0-9+]{9,15}$/)],
    ],
    shippingAddress: ['', [Validators.required, Validators.maxLength(255)]],
    defaultAddress: [false],
  });
  ngOnInit(): void {
    this.destroyRef.onDestroy(() => this.editorDialog?.close());
    this.load();
  }

  load(): void {
    this.loading = true;
    this.error = '';
    this.service
      .findMine()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (value) => {
          this.addresses = value;
          this.loading = false;
        },
        error: (response) => {
          this.error = this.errorMessages.message(
            response,
            'account.cannotLoadAddresses',
          );
          this.loading = false;
        },
      });
  }

  edit(address?: Address): void {
    if (this.saving || this.editorDialog) {
      return;
    }
    this.editingId = address?.id;
    this.formError = '';
    this.form.reset({
      recipientName: address?.recipientName ?? '',
      phoneNumber: address?.phoneNumber ?? '',
      shippingAddress: address?.shippingAddress ?? '',
      defaultAddress: address?.defaultAddress ?? false,
    });
    this.editorDialog = this.dialog.open(this.editor, {
      width: '560px',
      maxWidth: 'calc(100vw - 32px)',
      disableClose: false,
      ariaLabelledBy: 'address-title',
    });
    const editorDialog = this.editorDialog;
    editorDialog
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => {
        if (this.editorDialog === editorDialog) {
          this.editorDialog = undefined;
        }
      });
  }

  close(event?: Event): void {
    event?.preventDefault();
    if (!this.saving) {
      this.editorDialog?.close();
      this.editorDialog = undefined;
    }
  }

  save(): void {
    if (this.saving || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    if (this.editorDialog) {
      this.editorDialog.disableClose = true;
    }
    this.form.disable({ emitEvent: false });
    this.formError = '';
    this.service
      .save(this.form.getRawValue(), this.editingId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.saving = false;
          this.form.enable({ emitEvent: false });
          this.close();
          this.load();
        },
        error: (response) => {
          this.saving = false;
          if (this.editorDialog) {
            this.editorDialog.disableClose = false;
          }
          this.form.enable({ emitEvent: false });
          this.formError = this.errorMessages.message(
            response,
            'account.cannotSaveTheAddressCheckTheInformationAndThe',
          );
        },
      });
  }

  private removeConfirmed(address: Address): void {
    this.service
      .delete(address.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => this.load(),
        error: (response) => {
          this.error = this.errorMessages.message(
            response,
            'account.cannotDeleteTheAddress',
          );
        },
      });
  }

  remove(address: Address): void {
    if (this.confirmationOpen) {
      return;
    }
    this.confirmationOpen = true;
    this.dialog
      .open(ConfirmationDialogComponent, {
        data: this.translate.instant('account.deleteThisAddress'),
        width: '440px',
        maxWidth: 'calc(100vw - 32px)',
      })
      .afterClosed()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((confirmed: boolean | undefined) => {
        this.confirmationOpen = false;
        if (confirmed === true) {
          this.removeConfirmed(address);
        }
      });
  }
}
