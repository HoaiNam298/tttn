import { CommonModule } from '@angular/common';
import {
  Component,
  OnInit,
  inject,
  ChangeDetectionStrategy,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { CatalogService } from '../../services/catalog.service';
import { Product } from '../../models/product.model';
import { CartService } from '../../services/cart.service';

@Component({
  selector: 'app-product-detail',
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './product-detail.component.html',
})
export class ProductDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly catalog = inject(CatalogService);
  private readonly cart = inject(CartService);

  product?: Product;
  error = '';
  added = false;
  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.catalog.product(id).subscribe({
      next: (value) => (this.product = value),
      error: () => (this.error = 'Không tìm thấy sản phẩm.'),
    });
  }

  addToCart(): void {
    if (!this.product) {
      return;
    }
    this.cart.add(this.product.id);
    this.added = true;
  }
}
