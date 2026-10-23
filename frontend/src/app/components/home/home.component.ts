import { ErrorMessageService } from '../../services/error-message.service';
import { TranslatePipe } from '@ngx-translate/core';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { HomeSpotlightComponent } from './home-spotlight.component';
import { MatButtonModule } from '@angular/material/button';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  inject,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { CatalogService } from '../../services/catalog.service';
import { Product } from '../../models/product.model';
import { Category } from '../../models/category.model';
import { ProductCardComponent } from '../shared/product-card.component';

@Component({
  selector: 'app-home',
  imports: [
    TranslatePipe,
    RouterLink,
    ProductCardComponent,
    MatButtonModule,
    HomeSpotlightComponent,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  private readonly errorMessages = inject(ErrorMessageService);
  private readonly catalog = inject(CatalogService);
  private readonly destroyRef = inject(DestroyRef);
  categories: Category[] = [];
  products: Product[] = [];
  loading = true;
  error = '';
  categoryError = '';
  categoriesLoading = true;

  get featuredProduct(): Product | undefined {
    return (
      this.products.find(
        (product) => product.category.name.toLowerCase() === 'laptop',
      ) ?? this.products[0]
    );
  }

  categoryIcon(category: Category): string {
    const name = category.name
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .replace(/đ/g, 'd');
    const paths: Record<string, string> = {
      'dien thoai': 'M8 2h8v20H8z M11 18h2',
      laptop: 'M4 4h16v12H4z M2 20h20l-2-4H4z',
      'may tinh bang': 'M5 2h14v20H5z M11 18h2',
      'dong ho thong minh':
        'M9 2h6l1 5H8z M8 17h8l-1 5H9z M7 7h10v10H7z M12 9v3l2 1',
      'tai nghe': 'M4 14v-3a8 8 0 0 1 16 0v3 M4 12h3v8H4z M17 12h3v8h-3z',
      'man hinh': 'M2 3h20v14H2z M12 17v4 M7 21h10',
      'ban phim':
        'M2 6h20v12H2z M5 9h1m3 0h1m3 0h1m3 0h2 M5 12h1m3 0h1m3 0h1m3 0h2 M6 15h12',
      'chuot may tinh': 'M6 10a6 6 0 0 1 12 0v5a6 6 0 0 1-12 0z M12 4v6',
      'thiet bi mang':
        'M3 14h18v7H3z M7 17h1m3 0h1 M6 14V5m12 9V5 M8 8a6 6 0 0 1 8 0 M10 11a3 3 0 0 1 4 0',
    };
    return paths[name] ?? 'M3 7h18v14H3z M3 7l9-5 9 5 M12 7v14';
  }

  ngOnInit(): void {
    this.loadCategories();
    this.loadProducts();
  }

  loadCategories(): void {
    this.categoriesLoading = true;
    this.categoryError = '';
    this.catalog
      .categories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (value) => {
          this.categories = value;
          this.categoriesLoading = false;
        },
        error: (response) => {
          this.categoryError = this.errorMessages.message(
            response,
            'home.categoriesCouldNotBeLoaded',
          );
          this.categoriesLoading = false;
        },
      });
  }

  loadProducts(): void {
    this.loading = true;
    this.error = '';
    this.catalog
      .products('', undefined, 0, 12)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (value) => {
          this.products = value.content;
          this.loading = false;
        },
        error: (response) => {
          this.error = this.errorMessages.message(
            response,
            'home.cannotLoadProductsPleaseTryAgainLater',
          );
          this.loading = false;
        },
      });
  }
}
