import { CommonModule } from '@angular/common';
import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
  DestroyRef,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EMPTY, catchError, switchMap } from 'rxjs';
import { CatalogService } from '../../services/catalog.service';
import { Category } from '../../models/category.model';
import { Product } from '../../models/product.model';
import { ProductSort } from '../../models/product-filter.model';
import { ProductCardComponent } from '../shared/product-card.component';

@Component({
  selector: 'app-product-list',
  imports: [
    CommonModule,
    ReactiveFormsModule,
    RouterLink,
    ProductCardComponent,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './product-list.component.html',
  styleUrl: './product-list.component.scss',
})
export class ProductListComponent implements OnInit {
  private readonly catalog = inject(CatalogService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly fb = inject(FormBuilder);
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
  loading = true;
  error = '';
  categoryError = '';
  filterError = '';

  ngOnInit(): void {
    this.catalog
      .categories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (value) => {
          this.categories = value;
        },
        error: () => {
          this.categoryError = 'Không tải được danh mục.';
        },
      });
    this.route.queryParamMap
      .pipe(
        switchMap((params) => {
          const positive = (value: string | null): number | null => {
            const number = Number(value);
            return value !== null && Number.isFinite(number) && number >= 0
              ? number
              : null;
          };
          const sort = params.get('sort');
          const category = positive(params.get('categoryId'));
          this.form.reset({
            keyword: params.get('keyword') ?? '',
            categoryId:
              category && Number.isInteger(category) ? category : null,
            minPrice: positive(params.get('minPrice')),
            maxPrice: positive(params.get('maxPrice')),
            sort: sort === 'priceAsc' || sort === 'priceDesc' ? sort : 'newest',
          });
          const page = positive(params.get('page')) ?? 0;
          this.loading = true;
          this.error = '';
          this.filterError = '';
          const value = this.form.getRawValue();
          return this.catalog
            .products(
              value.keyword ?? '',
              value.categoryId ?? undefined,
              Number.isInteger(page) ? page : 0,
              12,
              {
                minPrice: value.minPrice ?? undefined,
                maxPrice: value.maxPrice ?? undefined,
                sort: value.sort ?? 'newest',
              },
            )
            .pipe(
              catchError(() => {
                this.error =
                  'Không thể tải sản phẩm. Kiểm tra bộ lọc hoặc thử lại.';
                this.products = [];
                this.totalPages = 0;
                this.loading = false;
                return EMPTY;
              }),
            );
        }),
      )
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
    const value = this.form.getRawValue();
    if (
      this.form.invalid ||
      (value.minPrice != null &&
        value.maxPrice != null &&
        value.minPrice > value.maxPrice)
    ) {
      this.filterError =
        'Khoảng giá không hợp lệ. Giá tối thiểu phải nhỏ hơn hoặc bằng giá tối đa.';
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
}
