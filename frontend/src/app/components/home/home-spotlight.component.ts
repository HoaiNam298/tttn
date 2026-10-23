import {
  ChangeDetectionStrategy,
  Component,
  OnChanges,
  input,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { TranslatePipe } from '@ngx-translate/core';
import { Product } from '../../models/product.model';
import { ShopMoneyPipe } from '../shared/shop-money.pipe';

@Component({
  selector: 'app-home-spotlight',
  imports: [RouterLink, TranslatePipe, ShopMoneyPipe],
  templateUrl: './home-spotlight.component.html',
  styleUrl: './home-spotlight.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class HomeSpotlightComponent implements OnChanges {
  readonly product = input<Product>();
  readonly loading = input(false);
  imageFailed = false;

  ngOnChanges(): void {
    this.imageFailed = false;
  }
}
