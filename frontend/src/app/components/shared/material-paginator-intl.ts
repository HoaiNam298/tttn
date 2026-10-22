import { MatPaginatorIntl } from '@angular/material/paginator';
import { effect, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';

export function createMaterialPaginatorIntl(): MatPaginatorIntl {
  const intl = new MatPaginatorIntl();
  const translate = inject(TranslateService);
  effect(() => {
    translate.currentLang();
    intl.itemsPerPageLabel = translate.instant('pagination.rows');
    intl.nextPageLabel = translate.instant('pagination.next');
    intl.previousPageLabel = translate.instant('pagination.previous');
    intl.firstPageLabel = translate.instant('pagination.first');
    intl.lastPageLabel = translate.instant('pagination.last');
    intl.changes.next();
  });
  intl.getRangeLabel = (page, size, length): string => {
    if (!length) {
      return '0 / 0';
    }
    return `${page * size + 1}–${Math.min((page + 1) * size, length)} / ${length}`;
  };
  return intl;
}
