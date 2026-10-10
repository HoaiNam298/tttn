import { MatPaginatorIntl } from '@angular/material/paginator';

export function createMaterialPaginatorIntl(): MatPaginatorIntl {
  const intl = new MatPaginatorIntl();
  intl.itemsPerPageLabel = 'Số dòng / trang';
  intl.nextPageLabel = 'Trang sau';
  intl.previousPageLabel = 'Trang trước';
  intl.firstPageLabel = 'Trang đầu';
  intl.lastPageLabel = 'Trang cuối';
  intl.getRangeLabel = (page, size, length): string => {
    if (!length) {
      return '0 / 0';
    }
    return `${page * size + 1}–${Math.min((page + 1) * size, length)} / ${length}`;
  };
  return intl;
}
