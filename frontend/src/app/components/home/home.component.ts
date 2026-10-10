import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCardModule } from '@angular/material/card';
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
    RouterLink,
    ProductCardComponent,
    MatButtonModule,
    MatCardModule,
    MatProgressBarModule,
  ],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './home.component.html',
  styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
  private readonly catalog = inject(CatalogService);
  private readonly destroyRef = inject(DestroyRef);
  categories: Category[] = [];
  products: Product[] = [];
  loading = true;
  error = '';
  categoryError = '';

  ngOnInit(): void {
    this.catalog
      .categories()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (value) => {
          this.categories = value;
        },
        error: () => {
          this.categoryError = 'Chưa tải được danh mục.';
        },
      });
    this.catalog
      .products('', undefined, 0, 12)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (value) => {
          this.products = value.content;
          this.loading = false;
        },
        error: () => {
          this.error = 'Không thể tải sản phẩm. Vui lòng thử lại sau.';
          this.loading = false;
        },
      });
  }
}
