import { ShopMoneyPipe } from './shop-money.pipe';
import { TranslatePipe } from '@ngx-translate/core';
import {
  ChangeDetectionStrategy,
  Component,
  OnChanges,
  input,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { Product } from '../../models/product.model';
import { MatCardModule } from '@angular/material/card';

@Component({
  selector: 'app-product-card',
  imports: [ShopMoneyPipe, TranslatePipe, RouterLink, MatCardModule],
  changeDetection: ChangeDetectionStrategy.OnPush,
  templateUrl: './product-card.component.html',
  styleUrl: './product-card.component.scss',
})
export class ProductCardComponent implements OnChanges {
  readonly product = input.required<Product>();
  imageFailed = false;

  ngOnChanges(): void {
    this.imageFailed = false;
  }
}
