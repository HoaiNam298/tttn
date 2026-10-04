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

@Component({
  selector: 'app-product-detail',
  imports: [CommonModule, RouterLink],
  changeDetection: ChangeDetectionStrategy.Eager,
  templateUrl: './product-detail.component.html',
})
export class ProductDetailComponent implements OnInit {
  private readonly route = inject(ActivatedRoute);
  private readonly catalog = inject(CatalogService);

  product?: Product;
  error = '';
  ngOnInit(): void {
    const id = Number(this.route.snapshot.paramMap.get('id'));
    this.catalog.product(id).subscribe({
      next: (value) => (this.product = value),
      error: () => (this.error = 'Không tìm thấy sản phẩm.'),
    });
  }
}
