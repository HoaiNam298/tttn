import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CatalogService } from '../../../services/catalog.service';
import { CategoryManagementComponent } from './category-management.component';

describe('Material category management', () => {
  const category = { id: 1, name: 'Điện thoại' };

  function setup() {
    const catalog = {
      categoryPage: vi.fn(() =>
        of({ content: [category], number: 0, totalPages: 1, totalElements: 1 }),
      ),
      createCategory: vi.fn(() => of(category)),
      updateCategory: vi.fn(() => of(category)),
    };
    TestBed.configureTestingModule({
      imports: [CategoryManagementComponent],
      providers: [{ provide: CatalogService, useValue: catalog }],
    });
    const fixture = TestBed.createComponent(CategoryManagementComponent);
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance, catalog };
  }

  it('renders Material table and paginator and forwards page size to API', () => {
    const { fixture, component, catalog } = setup();
    expect(
      fixture.nativeElement.querySelector('table[mat-table]'),
    ).toBeTruthy();
    expect(fixture.nativeElement.querySelector('mat-paginator')).toBeTruthy();
    component.changePage({ pageIndex: 0, pageSize: 20, length: 1 });
    expect(catalog.categoryPage).toHaveBeenLastCalledWith('', 0, 20);
  });

  it('opens the editor with existing data and closes it on successful save', () => {
    const { fixture, component, catalog } = setup();
    component.edit(category);
    fixture.detectChanges();
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    expect(component.form.controls.name.value).toBe(category.name);
    component.form.controls.name.setValue('Laptop');
    component.create();
    expect(catalog.updateCategory).toHaveBeenCalledWith(1, 'Laptop');
    expect(component.editingId).toBeUndefined();
    expect(component.message).toContain('Đã cập nhật');
  });

  it('keeps editor open and displays error after failed save', () => {
    const { component, catalog } = setup();
    catalog.updateCategory.mockReturnValue(
      throwError(() => new Error('conflict')),
    );
    component.edit(category);
    component.create();
    expect(component.saving).toBe(false);
    expect(component.formError).not.toBe('');
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    component.cancelEdit();
  });

  it('prevents duplicate saves and closing while a request is pending', () => {
    const { component, catalog } = setup();
    const pending = new Subject<typeof category>();
    catalog.updateCategory.mockReturnValue(pending);
    component.edit(category);
    component.create();
    component.create();
    component.cancelEdit();
    expect(catalog.updateCategory).toHaveBeenCalledTimes(1);
    expect(component.editingId).toBe(1);
    pending.next(category);
    pending.complete();
  });
});
