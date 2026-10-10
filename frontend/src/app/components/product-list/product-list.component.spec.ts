import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap } from '@angular/router';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';
import { CatalogService } from '../../services/catalog.service';
import { ProductListComponent } from './product-list.component';

describe('Catalog filters and request ordering', () => {
  const catalog = { categories: vi.fn(), products: vi.fn() };
  const router = { navigate: vi.fn() };
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  beforeEach(() => {
    params = new BehaviorSubject(
      convertToParamMap({
        categoryId: '2',
        minPrice: '0',
        maxPrice: '500',
        sort: 'priceAsc',
        page: '1',
      }),
    );
    catalog.categories.mockReturnValue(of([]));
    catalog.products.mockReturnValue(
      of({ content: [], number: 1, totalPages: 3, totalElements: 25 }),
    );
    router.navigate.mockReset();
    catalog.products.mockClear();
    TestBed.configureTestingModule({
      providers: [
        { provide: CatalogService, useValue: catalog },
        { provide: Router, useValue: router },
        { provide: ActivatedRoute, useValue: { queryParamMap: params } },
      ],
    });
  });

  it('restores filters and pagination from the URL', () => {
    const component = TestBed.runInInjectionContext(
      () => new ProductListComponent(),
    );
    component.ngOnInit();
    expect(catalog.products).toHaveBeenCalledWith('', 2, 1, 12, {
      minPrice: 0,
      maxPrice: 500,
      sort: 'priceAsc',
    });
    expect(component.form.controls.minPrice.value).toBe(0);
    expect(component.totalElements).toBe(25);
  });

  it('rejects reversed price ranges without navigating', () => {
    const component = TestBed.runInInjectionContext(
      () => new ProductListComponent(),
    );
    component.form.patchValue({ minPrice: 600, maxPrice: 100 });
    component.load();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(component.filterError).toBeTruthy();
  });

  it('does not let an older request overwrite newer search results', () => {
    const oldRequest = new Subject<unknown>();
    const newRequest = new Subject<unknown>();
    catalog.products
      .mockReturnValueOnce(oldRequest)
      .mockReturnValueOnce(newRequest);
    const component = TestBed.runInInjectionContext(
      () => new ProductListComponent(),
    );
    component.ngOnInit();
    params.next(convertToParamMap({ keyword: 'new' }));
    newRequest.next({
      content: [],
      number: 0,
      totalPages: 1,
      totalElements: 2,
    });
    oldRequest.next({
      content: [],
      number: 1,
      totalPages: 99,
      totalElements: 999,
    });
    expect(component.totalElements).toBe(2);
  });

  it('retries a failed search without requiring a different URL', () => {
    catalog.products
      .mockReturnValueOnce(throwError(() => new Error('offline')))
      .mockReturnValueOnce(
        of({ content: [], number: 1, totalPages: 3, totalElements: 25 }),
      );
    const component = TestBed.runInInjectionContext(
      () => new ProductListComponent(),
    );
    component.ngOnInit();
    expect(component.error).not.toBe('');
    expect(component.totalElements).toBe(0);
    component.retry();
    expect(catalog.products).toHaveBeenCalledTimes(2);
    expect(catalog.products).toHaveBeenLastCalledWith('', 2, 1, 12, {
      minPrice: 0,
      maxPrice: 500,
      sort: 'priceAsc',
    });
    expect(router.navigate).not.toHaveBeenCalled();
    expect(component.error).toBe('');
    expect(component.totalElements).toBe(25);
  });

  it('preserves active filters when moving to another paginator page', () => {
    const component = TestBed.runInInjectionContext(
      () => new ProductListComponent(),
    );
    component.ngOnInit();
    component.load(2);
    expect(router.navigate).toHaveBeenCalledWith(['/products'], {
      queryParams: {
        keyword: null,
        categoryId: 2,
        minPrice: 0,
        maxPrice: 500,
        sort: 'priceAsc',
        page: 2,
      },
    });
  });
});
