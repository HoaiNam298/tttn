import { Pipe, PipeTransform, inject } from '@angular/core';
import { DisplayFormatService } from '../../services/display-format.service';

@Pipe({ name: 'shopNumber', pure: false })
export class ShopNumberPipe implements PipeTransform {
  private readonly format = inject(DisplayFormatService);
  transform(value: number | null | undefined, digits = '1.0-3'): string {
    return this.format.number(value, digits);
  }
}
