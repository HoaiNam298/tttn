import { ErrorMessageService } from '../../services/error-message.service';
import { TranslatePipe } from '@ngx-translate/core';
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
    TranslatePipe,
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
  private readonly errorMessages = inject(ErrorMessageService);
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
        error: (response) => {
          this.categoryError = this.errorMessages.message(
            response,
            'home.categoriesCouldNotBeLoaded',
          );
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
