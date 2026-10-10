import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of, Subject } from 'rxjs';
import { AddressService } from '../../../services/address.service';
import { AddressesComponent } from './addresses.component';

describe('Material address editor', () => {
  const address = {
    id: 1,
    recipientName: 'Customer',
    phoneNumber: '0901234567',
    shippingAddress: 'Test address',
    defaultAddress: true,
  };
  const service = { findMine: vi.fn(), save: vi.fn(), delete: vi.fn() };
  beforeEach(() => {
    vi.resetAllMocks();
    service.findMine.mockReturnValue(of([address]));
    service.save.mockReturnValue(of(address));
    service.delete.mockReturnValue(of(undefined));
    TestBed.configureTestingModule({
      imports: [AddressesComponent],
      providers: [{ provide: AddressService, useValue: service }],
    });
  });
  it('opens a Material dialog preserving the address ID and default flag', () => {
    const fixture = TestBed.createComponent(AddressesComponent);
    fixture.detectChanges();
    fixture.componentInstance.edit(address);
    fixture.detectChanges();
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    expect(document.querySelector('mat-checkbox')).not.toBeNull();
    fixture.componentInstance.save();
    expect(service.save).toHaveBeenCalledWith(
      {
        recipientName: address.recipientName,
        phoneNumber: address.phoneNumber,
        shippingAddress: address.shippingAddress,
        defaultAddress: true,
      },
      1,
    );
  });
  it('marks invalid fields instead of calling save', () => {
    const fixture = TestBed.createComponent(AddressesComponent);
    fixture.detectChanges();
    fixture.componentInstance.edit();
    fixture.componentInstance.save();
    expect(service.save).not.toHaveBeenCalled();
    expect(fixture.componentInstance.form.touched).toBe(true);
  });
  it('freezes the form during save and allows retry after a failure', () => {
    const pending = new Subject();
    service.save.mockReturnValue(pending);
    const fixture = TestBed.createComponent(AddressesComponent);
    fixture.detectChanges();
    fixture.componentInstance.edit(address);
    fixture.componentInstance.save();
    fixture.componentInstance.save();
    expect(service.save).toHaveBeenCalledTimes(1);
    expect(fixture.componentInstance.form.disabled).toBe(true);
    expect(TestBed.inject(MatDialog).openDialogs[0].disableClose).toBe(true);
    pending.error(new Error('Conflict'));
    expect(fixture.componentInstance.form.enabled).toBe(true);
    expect(fixture.componentInstance.formError).not.toBe('');
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    expect(TestBed.inject(MatDialog).openDialogs[0].disableClose).toBe(false);
  });
  it('allows opening the editor again after dismissing the dialog', async () => {
    const fixture = TestBed.createComponent(AddressesComponent);
    fixture.detectChanges();
    const dialog = TestBed.inject(MatDialog);
    fixture.componentInstance.edit(address);
    expect(dialog.openDialogs[0].disableClose).toBe(false);
    dialog.openDialogs[0].close();
    await vi.waitFor(() => expect(dialog.openDialogs).toHaveLength(0));
    fixture.componentInstance.edit();
    expect(dialog.openDialogs).toHaveLength(1);
    expect(fixture.componentInstance.editingId).toBeUndefined();
  });
  it('only deletes after confirmation, not after dismissal', async () => {
    const fixture = TestBed.createComponent(AddressesComponent);
    fixture.detectChanges();
    const dialog = TestBed.inject(MatDialog);
    fixture.componentInstance.remove(address);
    expect(service.delete).not.toHaveBeenCalled();
    dialog.openDialogs[0].close(false);
    await vi.waitFor(() => expect(dialog.openDialogs).toHaveLength(0));
    expect(service.delete).not.toHaveBeenCalled();
    fixture.componentInstance.remove(address);
    dialog.openDialogs[0].close(true);
    await vi.waitFor(() =>
      expect(service.delete).toHaveBeenCalledWith(address.id),
    );
    expect(service.delete).toHaveBeenCalledTimes(1);
  });
  it('closes the address editor when the page is destroyed', () => {
    const fixture = TestBed.createComponent(AddressesComponent);
    fixture.detectChanges();
    fixture.componentInstance.edit(address);
    const close = vi.spyOn(TestBed.inject(MatDialog).openDialogs[0], 'close');
    fixture.destroy();
    expect(close).toHaveBeenCalled();
  });
});
