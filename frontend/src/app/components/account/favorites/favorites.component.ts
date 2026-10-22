import { ErrorMessageService } from '../../../services/error-message.service';
import { TranslatePipe } from '@ngx-translate/core';
import {
  MatPaginatorIntl,
  MatPaginatorModule,
} from '@angular/material/paginator';
import { createMaterialPaginatorIntl } from '../../shared/material-paginator-intl';

import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { CommonModule } from '@angular/common';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  OnInit,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FavoriteService } from '../../../services/favorite.service';
import { Product } from '../../../models/product.model';
import { ProductCardComponent } from '../../shared/product-card.component';

@Component({
  providers: [
    { provide: MatPaginatorIntl, useFactory: createMaterialPaginatorIntl },
  ],
  selector: 'app-favorites',
  imports: [
    TranslatePipe,
    CommonModule,
    ProductCardComponent,
    MatButtonModule,
    MatCardModule,
    MatProgressBarModule,
    MatPaginatorModule,
  ],
  templateUrl: './favorites.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class FavoritesComponent implements OnInit {
  private readonly errorMessages = inject(ErrorMessageService);
  private readonly service = inject(FavoriteService);
  private readonly destroyRef = inject(DestroyRef);
  private revision = 0;
  products: Product[] = [];
  page = 0;
  totalPages = 0;
  totalElements = 0;
  loading = false;
  removingId?: number;
  error = '';

  ngOnInit(): void {
    this.load();
  }

  load(page = 0): void {
    const revision = ++this.revision;
    this.loading = true;
    this.error = '';
    this.service
      .list(page)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (result) => {
          if (revision !== this.revision) {
            return;
          }
          this.products = result.content;
          this.page = result.number;
          this.totalPages = result.totalPages;
          this.totalElements = result.totalElements;
          this.loading = false;
          if (!result.content.length && page > 0) {
            this.load(page - 1);
          }
        },
        error: (response) => {
          if (revision === this.revision) {
            this.error = this.errorMessages.message(
              response,
              'account.cannotLoadFavoritesPleaseTryAgain',
            );
            this.loading = false;
          }
        },
      });
  }

  remove(id: number): void {
    if (this.removingId !== undefined) {
      return;
    }
    this.removingId = id;
    this.service
      .set(id, false)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.removingId = undefined;
          this.load(this.page);
        },
        error: (response) => {
          this.removingId = undefined;
          this.error = this.errorMessages.message(
            response,
            'account.cannotRemoveThisFavorite',
          );
        },
      });
  }
}
