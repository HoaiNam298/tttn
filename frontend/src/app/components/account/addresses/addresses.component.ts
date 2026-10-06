import {
  ChangeDetectionStrategy,
  Component,
  ElementRef,
  OnInit,
  ViewChild,
  inject,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Address } from '../../../models/address.model';
import { AddressService } from '../../../services/address.service';

@Component({
  selector: 'app-addresses',
  imports: [ReactiveFormsModule],
  templateUrl: './addresses.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class AddressesComponent implements OnInit {
  private readonly service = inject(AddressService);
  private readonly fb = inject(FormBuilder);
  @ViewChild('editor') private editor!: ElementRef<HTMLDialogElement>;
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
    this.load();
  }
  load(): void {
    this.loading = true;
    this.error = '';
    this.service.findMine().subscribe({
      next: (value) => {
        this.addresses = value;
        this.loading = false;
      },
      error: () => {
        this.error = 'Không thể tải địa chỉ.';
        this.loading = false;
      },
    });
  }
  edit(address?: Address): void {
    this.editingId = address?.id;
    this.formError = '';
    this.form.reset({
      recipientName: address?.recipientName ?? '',
      phoneNumber: address?.phoneNumber ?? '',
      shippingAddress: address?.shippingAddress ?? '',
      defaultAddress: address?.defaultAddress ?? false,
    });
    this.editor.nativeElement.showModal();
  }
  close(event?: Event): void {
    event?.preventDefault();
    if (!this.saving) {
      this.editor.nativeElement.close();
    }
  }
  save(): void {
    if (this.saving || this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.saving = true;
    this.formError = '';
    this.service.save(this.form.getRawValue(), this.editingId).subscribe({
      next: () => {
        this.saving = false;
        this.close();
        this.load();
      },
      error: () => {
        this.saving = false;
        this.formError =
          'Không thể lưu địa chỉ. Kiểm tra thông tin và giới hạn 20 địa chỉ.';
      },
    });
  }
  remove(address: Address): void {
    if (!confirm('Xóa địa chỉ này?')) {
      return;
    }
    this.service.delete(address.id).subscribe({
      next: () => this.load(),
      error: () => {
        this.error = 'Không thể xóa địa chỉ.';
      },
    });
  }
}
