import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { CatalogService } from '../../services/catalog.service';
import { CartService } from '../../services/cart.service';
import { ProductDetailComponent } from './product-detail.component';
import { Product } from '../../models/product.model';

describe('Product detail navigation and gallery', () => {
  it('reloads on product-id changes and excludes the current product from related cards', () => {
    const params = new BehaviorSubject(convertToParamMap({ id: '8' }));
    const product = (id: number): Product => ({
      id,
      name: `Product ${id}`,
      price: 100,
      stock: 10,
      category: { id: 2, name: 'Phone' },
      description: '',
      thumbnail: 'front.jpg',
      images: ['front.jpg', 'back.jpg'],
      createdAt: '',
      updatedAt: '',
    });
    TestBed.configureTestingModule({
      providers: [
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: params,
            queryParamMap: of(convertToParamMap({})),
          },
        },
        { provide: Router, useValue: { navigate: vi.fn() } },
        { provide: CartService, useValue: { add: vi.fn() } },
        {
          provide: CatalogService,
          useValue: {
            product: (id: number) => of(product(id)),
            products: () => of({ content: [product(8), product(9)] }),
            reviews: () =>
              of({
                reviews: { content: [], number: 0, totalPages: 0 },
                averageRating: 0,
                totalReviews: 0,
              }),
          },
        },
      ],
    });
    const component = TestBed.runInInjectionContext(
      () => new ProductDetailComponent(),
    );
    component.ngOnInit();
    expect(component.images).toEqual(['front.jpg', 'back.jpg']);
    component.moveImage(-1);
    expect(component.selectedImage).toBe('back.jpg');
    expect(component.relatedProducts.map((item) => item.id)).toEqual([9]);
    component.quantity = 5;
    params.next(convertToParamMap({ id: '9' }));
    expect(component.product?.id).toBe(9);
    expect(component.quantity).toBe(1);
    expect(component.relatedProducts.map((item) => item.id)).toEqual([8]);
  });
});
