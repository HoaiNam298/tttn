import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CatalogService } from '../../../services/catalog.service';
import { Product } from '../../../models/product.model';
import { ProductInventoryEditorComponent } from './product-inventory-editor.component';

describe('Material inventory editor', () => {
  const product: Product = {
    id: 1,
    name: 'Điện thoại',
    price: 100,
    thumbnail: '/main.jpg',
    description: '',
    category: { id: 1, name: 'Điện thoại' },
    createdAt: '',
    updatedAt: '',
    stock: 5,
    version: 7,
    images: ['/main.jpg', '/second.jpg'],
    variants: [
      {
        id: 11,
        sku: 'PHONE-BLACK',
        name: 'Đen',
        price: 100,
        stock: 5,
        color: 'Đen',
        size: '',
        capacity: '128 GB',
        imageUrl: null,
        active: true,
      },
    ],
  };
  function setup() {
    const catalog = {
      product: vi.fn(() => of(product)),
      updateInventory: vi.fn(() => of(product)),
      uploadImage: vi.fn(() => of({ url: '/uploaded.jpg' })),
    };
    TestBed.configureTestingModule({
      imports: [ProductInventoryEditorComponent],
      providers: [{ provide: CatalogService, useValue: catalog }],
    });
    const fixture = TestBed.createComponent(ProductInventoryEditorComponent);
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance, catalog };
  }
  function fileEvent(type = 'image/png'): Event {
    const input = document.createElement('input');
    input.type = 'file';
    Object.defineProperty(input, 'files', {
      value: [new File(['image'], 'image.png', { type })],
    });
    const event = new Event('change');
    Object.defineProperty(event, 'target', { value: input });
    return event;
  }

  it('opens Material Dialog with deduplicated gallery and current variant data', () => {
    const { fixture, component } = setup();
    component.open(1);
    fixture.detectChanges();
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    expect(component.images).toEqual(['/main.jpg', '/second.jpg']);
    expect(component.variants.at(0).controls.id.value).toBe(11);
    expect(document.querySelector('mat-expansion-panel')).toBeTruthy();
    component.close();
  });

  it('preserves existing SKU IDs and deactivates persisted variants instead of deleting', () => {
    const { component } = setup();
    component.open(1);
    component.removeVariant(0);
    expect(component.variants.length).toBe(1);
    expect(component.variants.at(0).controls.active.value).toBe(false);
    component.addVariant();
    component.removeVariant(1);
    expect(component.variants.length).toBe(1);
    component.close();
  });

  it('blocks gallery and SKU mutations while uploading, then maps the image to the same SKU', () => {
    const { component, catalog } = setup();
    const pending = new Subject<{ url: string }>();
    catalog.uploadImage.mockReturnValue(pending);
    component.open(1);
    component.upload(fileEvent(), 0);
    expect(component.form.disabled).toBe(true);
    component.removeVariant(0);
    component.addVariant();
    component.removeImage(0);
    component.makePrimary(1);
    expect(component.variants.length).toBe(1);
    expect(component.variants.at(0).controls.active.value).toBe(true);
    expect(component.images[0]).toBe('/main.jpg');
    pending.next({ url: '/sku.jpg' });
    pending.complete();
    expect(component.variants.at(0).controls.imageUrl.value).toBe('/sku.jpg');
    expect(component.form.enabled).toBe(true);
    component.close();
  });

  it('rejects invalid image MIME types and avoids uploads once the gallery is full', () => {
    const { component, catalog } = setup();
    component.open(1);
    component.upload(fileEvent('image/gif'));
    expect(catalog.uploadImage).not.toHaveBeenCalled();
    expect(component.error).toContain('JPEG/PNG');
    component.images = Array.from({ length: 10 }, (_, i) => `/image-${i}.jpg`);
    component.upload(fileEvent());
    expect(catalog.uploadImage).not.toHaveBeenCalled();
    component.close();
  });

  it('sends the original version and SKU IDs, locks the form and emits saved on success', () => {
    const { component, catalog } = setup();
    const pending = new Subject<Product>();
    catalog.updateInventory.mockReturnValue(pending);
    const saved = vi.fn();
    component.saved.subscribe(saved);
    component.open(1);
    component.makePrimary(1);
    component.save();
    component.save();
    expect(catalog.updateInventory).toHaveBeenCalledTimes(1);
    expect(catalog.updateInventory).toHaveBeenCalledWith(
      1,
      expect.objectContaining({
        version: 7,
        stock: 5,
        images: ['/second.jpg', '/main.jpg'],
        variants: [
          expect.objectContaining({ id: 11, sku: 'PHONE-BLACK', active: true }),
        ],
      }),
    );
    expect(component.form.disabled).toBe(true);
    pending.next(product);
    pending.complete();
    expect(saved).toHaveBeenCalledTimes(1);
  });

  it('keeps edits and re-enables the form after a conflict', () => {
    const { component, catalog } = setup();
    catalog.updateInventory.mockReturnValue(
      throwError(() => ({ error: { message: 'Tồn kho đã thay đổi' } })),
    );
    component.open(1);
    component.variants.at(0).controls.stock.setValue(8);
    component.save();
    expect(component.error).toBe('Tồn kho đã thay đổi');
    expect(component.form.enabled).toBe(true);
    expect(component.variants.at(0).controls.stock.value).toBe(8);
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    component.close();
  });
});
