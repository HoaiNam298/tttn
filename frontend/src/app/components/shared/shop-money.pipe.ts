import { Pipe, PipeTransform, inject } from '@angular/core';
import { DisplayFormatService } from '../../services/display-format.service';

@Pipe({ name: 'shopMoney', pure: false })
export class ShopMoneyPipe implements PipeTransform {
  private readonly format = inject(DisplayFormatService);
  transform(value: number | null | undefined): string {
    return this.format.money(value);
  }
}
