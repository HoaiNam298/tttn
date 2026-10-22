import { HttpErrorResponse } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { TimeoutError } from 'rxjs';
import { DOCUMENT } from '@angular/common';
import { isShopAppApi } from './shopapp-api-url';

@Injectable({ providedIn: 'root' })
export class ErrorMessageService {
  private readonly translate = inject(TranslateService);
  private readonly document = inject(DOCUMENT);

  message(error: unknown, fallbackKey = 'errors.generic'): string {
    if (error instanceof TimeoutError) {
      return this.translate.instant('errors.timeout');
    }
    if (error instanceof HttpErrorResponse) {
      if (error.status === 0) {
        return this.translate.instant('errors.network');
      }
      const ownApi = isShopAppApi(error.url, this.document.baseURI);
      if (
        ownApi &&
        typeof error.error?.message === 'string' &&
        error.error.message.trim()
      ) {
        return error.error.message;
      }
      if (!ownApi && error.error?.error === 'invalid_grant') {
        return this.translate.instant('errors.credentials');
      }
    }
    return this.translate.instant(fallbackKey);
  }
}
