import { Pipe, PipeTransform, inject } from '@angular/core';
import { DisplayFormatService } from '../../services/display-format.service';

@Pipe({ name: 'shopDate', pure: false })
export class ShopDatePipe implements PipeTransform {
  private readonly format = inject(DisplayFormatService);
  transform(
    value: string | number | Date | null | undefined,
    mode = 'dateTime',
  ): string {
    return this.format.date(value, mode);
  }
}
