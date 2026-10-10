import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { VoucherService } from '../../../services/voucher.service';
import { Voucher } from '../../../models/voucher.model';
import { AdminVouchersComponent } from './admin-vouchers.component';

describe('Material voucher management', () => {
  const voucher: Voucher = {
    id: 1,
    code: 'DEMO10',
    type: 'PERCENT',
    amount: 10,
    minimumSubtotal: 0,
    maximumDiscount: 10000,
    startsAt: '2026-10-01T00:00:00Z',
    endsAt: '2026-10-31T00:00:00Z',
    usageLimit: 100,
    perUserLimit: 1,
    usedCount: 2,
    targetUserId: null,
    active: true,
    version: 3,
  };

  function setup() {
    const service = {
      findAll: vi.fn(() =>
        of({
          content: [voucher],
          number: 0,
          totalPages: 1,
          totalElements: 1,
          size: 10,
          first: true,
          last: true,
        }),
      ),
      save: vi.fn(() => of(voucher)),
    };
    TestBed.configureTestingModule({
      imports: [AdminVouchersComponent],
      providers: [{ provide: VoucherService, useValue: service }],
    });
    const fixture = TestBed.createComponent(AdminVouchersComponent);
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance, service };
  }

  it('renders a Material table and sends the selected page size to the API', () => {
    const { fixture, component, service } = setup();
    expect(
      fixture.nativeElement.querySelector('table[mat-table]'),
    ).toBeTruthy();
    component.changePage({ pageIndex: 0, pageSize: 50, length: 1 });
    expect(service.findAll).toHaveBeenLastCalledWith(0, 50);
  });

  it('preserves voucher ID and optimistic locking version on edit', () => {
    const { component, service } = setup();
    component.open(voucher);
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    component.save();
    expect(service.save).toHaveBeenCalledWith(
      expect.objectContaining({
        code: 'DEMO10',
        version: 3,
        startsAt: new Date(voucher.startsAt).toISOString(),
        endsAt: new Date(voucher.endsAt).toISOString(),
      }),
      1,
    );
    expect(component.saving).toBe(false);
  });

  it('uses distinct labels and badge classes for active and inactive vouchers', () => {
    const { fixture, component } = setup();
    component.result!.content = [
      voucher,
      { ...voucher, id: 2, code: 'DISABLED', active: false },
    ];
    fixture.detectChanges();
    expect(
      fixture.nativeElement.querySelector('.voucher-active').textContent,
    ).toContain('Đang bật');
    expect(
      fixture.nativeElement.querySelector('.voucher-inactive').textContent,
    ).toContain('Đã tắt');
  });

  it('rejects invalid percent discounts and reversed date ranges', () => {
    const { component, service } = setup();
    component.open(voucher);
    component.form.controls.amount.setValue(101);
    component.save();
    expect(service.save).not.toHaveBeenCalled();
    expect(component.editorError).not.toBe('');
    component.form.patchValue({
      amount: 10,
      endsAt: component.form.controls.startsAt.value,
    });
    component.save();
    expect(service.save).not.toHaveBeenCalled();
    component.close();
  });

  it('blocks duplicate saves and closing during the request', () => {
    const { component, service } = setup();
    const pending = new Subject<Voucher>();
    service.save.mockReturnValue(pending);
    component.open(voucher);
    component.save();
    component.save();
    component.close();
    expect(service.save).toHaveBeenCalledTimes(1);
    expect(component.saving).toBe(true);
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    pending.next(voucher);
    pending.complete();
  });

  it('retains dialog values and shows a failure message after a conflict', () => {
    const { component, service } = setup();
    service.save.mockReturnValue(
      throwError(() => ({ error: { message: 'Dữ liệu đã thay đổi' } })),
    );
    component.open(voucher);
    component.save();
    expect(component.editorError).toBe('Dữ liệu đã thay đổi');
    expect(component.form.controls.code.value).toBe(voucher.code);
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    component.close();
  });
});
