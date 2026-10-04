import { CommonModule } from '@angular/common';
import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { CatalogService } from '../../services/catalog.service';
import { Category } from '../../models/category.model';
import { Product } from '../../models/product.model';

@Component({
  selector: 'app-product-list',
  imports: [CommonModule, FormsModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './product-list.component.html',
})
export class ProductListComponent implements OnInit {
  private readonly catalog = inject(CatalogService);

  products: Product[] = [];
  categories: Category[] = [];
  keyword = '';
  categoryId?: number;
  page = 0;
  totalPages = 0;
  loading = true;
  error = '';
  ngOnInit(): void {
    this.catalog.categories().subscribe((value) => (this.categories = value));
    this.load();
  }
  load(page = 0): void {
    this.loading = true;
    this.error = '';
    this.catalog.products(this.keyword, this.categoryId, page).subscribe({
      next: (value) => {
        this.products = value.content;
        this.page = value.number;
        this.totalPages = value.totalPages;
        this.loading = false;
      },
      error: () => {
        this.error = 'Không thể tải danh sách sản phẩm.';
        this.loading = false;
      },
    });
  }
  clear(): void {
    this.keyword = '';
    this.categoryId = undefined;
    this.load();
  }
}
