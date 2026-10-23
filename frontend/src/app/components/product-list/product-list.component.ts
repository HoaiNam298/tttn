import { ErrorMessageService } from '../../services/error-message.service';
import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { CommonModule } from '@angular/common';
import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
  DestroyRef,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, ParamMap, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import {
  EMPTY,
  Observable,
  Subject,
  catchError,
  combineLatest,
  startWith,
  switchMap,
} from 'rxjs';
import { CatalogService } from '../../services/catalog.service';
import { Category } from '../../models/category.model';
import { Product } from '../../models/product.model';
import { PageResponse } from '../../responses/page.response';
import { ProductSort } from '../../models/product-filter.model';
import { ProductCardComponent } from '../shared/product-card.component';
import { ShopMoneyPipe } from '../shared/shop-money.pipe';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import {
  MatPaginatorIntl,
  MatPaginatorModule,
} from '@angular/material/paginator';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { createMaterialPaginatorIntl } from '../shared/material-paginator-intl';

@Component({
  selector: 'app-product-list',
  imports: [
    TranslatePipe,
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    ProductCardComponent,
    ShopMoneyPipe,
    MatButtonModule,
    MatCardModule,
    MatFormFieldModule,
    MatInputModule,
    MatSelectModule,
    MatPaginatorModule,
    MatProgressBarModule,
  ],
  providers: [
    { provide: MatPaginatorIntl, useFactory: createMaterialPaginatorIntl },
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss',
})
export class ProductListComponent implements OnInit {
  private readonly errorMessages = inject(ErrorMessageService);
  private readonly translate = inject(TranslateService);
  private readonly catalog = inject(CatalogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
  private readonly refresh = new Subject<void>();
  readonly form = this.fb.group({
    keyword: [''],
    categoryId: [null as number | null],
    minPrice: [null as number | null, Validators.min(0)],
    maxPrice: [null as number | null, Validators.min(0)],
    sort: ['newest' as ProductSort],
  });
  products: Product[] = [];
  categories: Category[] = [];
  page = 0;
  totalPages = 0;
  totalElements = 0;
  readonly pageSize = 12;
  loading = true;
  error = '';
  categoryError = '';
  filtersOpen = false;
  appliedFilters = this.form.getRawValue();
  private invalidRange = false;

  get filterError(): string {
    return this.invalidRange
      ? this.translate.instant(
          'catalog.invalidPriceRangeTheMinimumMustNotExceedThe',
        )
      : '';
  }

  get activeFilterCount(): number {
    const value = this.appliedFilters;
    return (
      Number(Boolean(value.keyword?.trim())) +
      Number(value.categoryId != null) +
      Number(value.minPrice != null || value.maxPrice != null)
    );
  }

  get appliedCategoryName(): string {
    return (
      this.categories.find(
        (category) => category.id === this.appliedFilters.categoryId,
      )?.name ?? this.translate.instant('catalog.category')
    );
  }

  ngOnInit(): void {
    this.catalog
      .categories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (value) => {
          this.categories = value;
        },
        error: (response) => {
          this.categoryError = this.errorMessages.message(
            response,
            'catalog.cannotLoadCategories',
          );
        },
      });
    combineLatest([
      this.route.queryParamMap,
      this.refresh.pipe(startWith(undefined)),
    ])
      .pipe(switchMap(([params]) => this.loadProducts(params)))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe((value) => {
        this.products = value.content;
        this.page = value.number;
        this.totalPages = value.totalPages;
        this.totalElements = value.totalElements;
        this.loading = false;
      });
  }

  load(page = 0): void {
    this.form.markAllAsTouched();
    const value = this.form.getRawValue();
    if (
      this.form.invalid ||
      (value.minPrice != null &&
        value.maxPrice != null &&
        value.minPrice > value.maxPrice)
    ) {
      this.invalidRange = true;
      this.filtersOpen = true;
      return;
    }
    void this.router.navigate(['/products'], {
      queryParams: {
        keyword: value.keyword?.trim() || null,
        categoryId: value.categoryId,
        minPrice: value.minPrice,
        maxPrice: value.maxPrice,
        sort: value.sort,
        page: page || null,
      },
    });
  }

  clear(): void {
    this.form.reset({ sort: 'newest', keyword: '' });
    this.load();
  }

  retry(): void {
    this.refresh.next();
  }

  private loadProducts(params: ParamMap): Observable<PageResponse<Product>> {
    const sort = params.get('sort');
    const category = this.parseNonNegativeNumber(params.get('categoryId'));
    this.form.reset({
      keyword: params.get('keyword') ?? '',
      categoryId: category && Number.isInteger(category) ? category : null,
      minPrice: this.parseNonNegativeNumber(params.get('minPrice')),
      maxPrice: this.parseNonNegativeNumber(params.get('maxPrice')),
      sort: sort === 'priceAsc' || sort === 'priceDesc' ? sort : 'newest',
    });
    const page = this.parseNonNegativeNumber(params.get('page')) ?? 0;
    this.loading = true;
    this.error = '';
    this.invalidRange = false;
    const value = this.form.getRawValue();
    this.appliedFilters = { ...value };
    return this.catalog
      .products(
        value.keyword ?? '',
        value.categoryId ?? undefined,
        Number.isInteger(page) ? page : 0,
        this.pageSize,
        {
          minPrice: value.minPrice ?? undefined,
          maxPrice: value.maxPrice ?? undefined,
          sort: value.sort ?? 'newest',
        },
      )
      .pipe(
        catchError(() => {
          this.error = this.translate.instant(
            'catalog.cannotLoadProductsCheckTheFiltersOrTryAgain',
          );
          this.products = [];
          this.totalPages = 0;
          this.totalElements = 0;
          this.loading = false;
          return EMPTY;
        }),
      );
  }

  private parseNonNegativeNumber(value: string | null): number | null {
    const number = Number(value);
    return value !== null && Number.isFinite(number) && number >= 0
      ? number
      : null;
  }
}
