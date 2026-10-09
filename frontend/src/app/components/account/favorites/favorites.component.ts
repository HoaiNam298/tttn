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
  selector: 'app-favorites',
  imports: [CommonModule, ProductCardComponent],
  templateUrl: './favorites.component.html',
  changeDetection: ChangeDetectionStrategy.Eager,
})
export class FavoritesComponent implements OnInit {
  private readonly service = inject(FavoriteService);
  private readonly destroyRef = inject(DestroyRef);
  private revision = 0;
  products: Product[] = [];
  page = 0;
  totalPages = 0;
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
          this.loading = false;
          if (!result.content.length && page > 0) {
            this.load(page - 1);
          }
        },
        error: () => {
          if (revision === this.revision) {
            this.error =
              'Không tải được danh sách yêu thích. Vui lòng thử lại.';
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
        error: () => {
          this.removingId = undefined;
          this.error = 'Không xóa được sản phẩm yêu thích.';
        },
      });
  }
}
