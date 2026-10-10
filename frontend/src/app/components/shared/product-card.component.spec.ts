import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ProductCardComponent } from './product-card.component';
import { Product } from '../../models/product.model';

describe('Material product cards', () => {
  const product: Product = {
    id: 8,
    name: 'ShopPhone',
    price: 1000000,
    thumbnail: '/phone.jpg',
    description: '',
    category: { id: 1, name: 'Điện thoại' },
    createdAt: '',
    updatedAt: '',
  };
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [ProductCardComponent],
      providers: [provideRouter([])],
    });
  });

  it('keeps the product link, name and category in a Material card', () => {
    const fixture = TestBed.createComponent(ProductCardComponent);
    fixture.componentRef.setInput('product', product);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('mat-card')).not.toBeNull();
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe(
      '/products/8',
    );
    expect(fixture.nativeElement.textContent).toContain('ShopPhone');
    expect(fixture.nativeElement.textContent).toContain('Điện thoại');
  });

  it('shows a fallback without removing navigation when an image fails', () => {
    const fixture = TestBed.createComponent(ProductCardComponent);
    fixture.componentRef.setInput('product', product);
    fixture.detectChanges();
    fixture.nativeElement
      .querySelector('img')
      .dispatchEvent(new Event('error'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('img')).toBeNull();
    expect(
      fixture.nativeElement.querySelector('.product-image').textContent,
    ).toContain('ShopApp');
    expect(fixture.nativeElement.querySelector('a').getAttribute('href')).toBe(
      '/products/8',
    );
  });
});
