import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { isShopAppApi } from '../services/shopapp-api-url';
import { LanguageService } from '../services/language.service';

export const languageInterceptor: HttpInterceptorFn = (request, next) => {
  if (!isShopAppApi(request.url, inject(DOCUMENT).baseURI)) {
    return next(request);
  }
  return next(
    request.clone({
      setHeaders: { 'Accept-Language': inject(LanguageService).current() },
    }),
  );
};
