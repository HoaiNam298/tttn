import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of, Subject } from 'rxjs';
import { vi } from 'vitest';
import { CatalogService } from '../../../services/catalog.service';
import { Product } from '../../../models/product.model';
import { AdminCatalogComponent } from './admin-catalog.component';

describe('Material product management', () => {
  const product: Product = {
    id: 1,
    name: 'Điện thoại',
    price: 100,
    thumbnail: null,
    description: 'Mô tả',
    category: { id: 2, name: 'Điện thoại' },
    createdAt: '',
    updatedAt: '',
  };

  function setup() {
    const catalog = {
      categories: vi.fn(() => of([product.category])),
      products: vi.fn(() =>
        of({ content: [product], number: 0, totalPages: 1, totalElements: 1 }),
      ),
      updateProduct: vi.fn(() => of(product)),
      createProduct: vi.fn(() => of(product)),
    };
    TestBed.configureTestingModule({
      imports: [AdminCatalogComponent],
      providers: [{ provide: CatalogService, useValue: catalog }],
    });
    const fixture = TestBed.createComponent(AdminCatalogComponent);
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance, catalog };
  }

  it('renders Material controls and preserves search filters while paging', () => {
    const { fixture, component, catalog } = setup();
    expect(fixture.nativeElement.querySelector('mat-paginator')).toBeTruthy();
    component.keyword = ' phone ';
    component.categoryId = 2;
    component.changePage({ pageIndex: 0, pageSize: 50, length: 1 });
    expect(catalog.products).toHaveBeenLastCalledWith('phone', 2, 0, 50);
  });

  it('maps the existing product to the dialog and saves the same product ID', () => {
    const { fixture, component, catalog } = setup();
    component.editProduct(product);
    fixture.detectChanges();
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    expect(component.productForm.controls.categoryId.value).toBe(2);
    component.saveProduct();
    expect(catalog.updateProduct).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ name: product.name, categoryId: 2 }),
    );
    expect(component.editingId).toBeUndefined();
  });

  it('does not call create API for an invalid form', () => {
    const { component, catalog } = setup();
    component.openCreate();
    component.saveProduct();
    expect(catalog.createProduct).not.toHaveBeenCalled();
    expect(component.productForm.touched).toBe(true);
    component.cancelEdit();
  });

  it('disables all form controls during saving and re-enables them on failure', () => {
    const { fixture, component, catalog } = setup();
    const pending = new Subject<Product>();
    catalog.updateProduct.mockReturnValue(pending);
    component.editProduct(product);
    fixture.detectChanges();
    component.saveProduct();
    expect(component.productForm.disabled).toBe(true);
    expect(catalog.updateProduct).toHaveBeenCalledWith(
      1,
      expect.objectContaining({ categoryId: 2 }),
    );
    component.saveProduct();
    expect(catalog.updateProduct).toHaveBeenCalledTimes(1);
    pending.error(new Error('conflict'));
    expect(component.productForm.enabled).toBe(true);
    expect(component.productForm.controls.name.value).toBe(product.name);
    expect(component.formError).not.toBe('');
    component.cancelEdit();
  });
});
