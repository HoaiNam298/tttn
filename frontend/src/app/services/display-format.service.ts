import { Injectable, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

@Injectable({ providedIn: 'root' })
export class DisplayFormatService {
  private readonly translate = inject(TranslateService);
  get locale(): string {
    return this.translate.currentLang() === 'en' ? 'en-US' : 'vi-VN';
  }

  money(value: number | null | undefined): string {
    if (value == null || !Number.isFinite(value)) {
      return '';
    }
    return new Intl.NumberFormat(this.locale, {
      style: 'currency',
      currency: 'VND',
      maximumFractionDigits: 0,
    }).format(value);
  }

  number(value: number | null | undefined, digits = '1.0-3'): string {
    if (value == null || !Number.isFinite(value)) {
      return '';
    }
    const match = /^(\d+)\.(\d+)-(\d+)$/.exec(digits);
    return new Intl.NumberFormat(this.locale, {
      minimumIntegerDigits: Number(match?.[1] ?? 1),
      minimumFractionDigits: Number(match?.[2] ?? 0),
      maximumFractionDigits: Number(match?.[3] ?? 3),
    }).format(value);
  }

  date(
    value: string | number | Date | null | undefined,
    mode = 'dateTime',
  ): string {
    if (value == null || value === '') {
      return '';
    }
    const date = new Date(value);
    if (!Number.isFinite(date.getTime())) {
      return '';
    }
    const options: Intl.DateTimeFormatOptions = {
      timeZone: 'Asia/Ho_Chi_Minh',
      month: '2-digit',
      day: '2-digit',
    };
    if (mode !== 'chart') {
      options.year = 'numeric';
    }
    if (mode === 'dateTime' || mode === 'seconds') {
      options.hour = '2-digit';
      options.minute = '2-digit';
      options.hourCycle = 'h23';
    }
    if (mode === 'seconds') {
      options.second = '2-digit';
    }
    const formatter = new Intl.DateTimeFormat(this.locale, options);
    if (mode === 'chart') {
      return formatter
        .formatToParts(date)
        .filter((part) => part.type === 'month' || part.type === 'day')
        .map((part) => part.value)
        .join('/');
    }
    return formatter.format(date);
  }

  compact(value: number): string {
    return new Intl.NumberFormat(this.locale, {
      notation: 'compact',
      maximumFractionDigits: 1,
    }).format(value);
  }
}
