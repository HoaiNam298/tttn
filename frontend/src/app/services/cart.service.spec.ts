import { TestBed } from '@angular/core/testing';
import { of } from 'rxjs';
import { Product } from '../models/product.model';
import { CartService } from './cart.service';
import { CatalogService } from './catalog.service';

describe('CartService', () => {
  let service: CartService;

  beforeEach(() => {
    localStorage.removeItem('shopapp_cart');
    TestBed.configureTestingModule({
      providers: [
        CartService,
        {
          provide: CatalogService,
          useValue: {
            product: (id: number) => of(product(id)),
          },
        },
      ],
    });
    service = TestBed.inject(CartService);
  });

  afterEach(() => localStorage.removeItem('shopapp_cart'));

  it('adds the same product by increasing its quantity', () => {
    service.add(1);
    service.add(1, 2);

    expect(service.count()).toBe(3);
    expect(service.items()).toEqual([{ productId: 1, quantity: 3 }]);
  });

  it('calculates cart lines from current catalog prices', () => {
    service.add(1, 2);

    service.lines().subscribe((lines) => {
      expect(lines[0].lineTotal).toBe(200000);
    });
  });

  function product(id: number): Product {
    return {
      id,
      name: 'Phone',
      price: 100000,
      thumbnail: '',
      description: '',
      category: { id: 1, name: 'Phone' },
      createdAt: '',
      updatedAt: '',
    };
  }
});
