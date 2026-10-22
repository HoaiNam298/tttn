import {
  ApplicationConfig,
  inject,
  provideAppInitializer,
  provideZoneChangeDetection,
} from '@angular/core';
import { AuthService } from './services/auth.service';
import { provideRouter } from '@angular/router';
import {
  provideHttpClient,
  withInterceptors,
  withXhr,
} from '@angular/common/http';
import { authInterceptor } from './interceptors/auth.interceptor';
import { languageInterceptor } from './interceptors/language.interceptor';
import { LanguageService } from './services/language.service';
import { provideTranslateService } from '@ngx-translate/core';
import { provideTranslateHttpLoader } from '@ngx-translate/http-loader';

import { routes } from './app.routes';

export const appConfig: ApplicationConfig = {
  providers: [
    provideTranslateService({
      fallbackLang: 'vi',
      lang: 'vi',
      loader: provideTranslateHttpLoader({
        prefix: '/i18n/',
        suffix: '.json',
        failOnError: true,
        enforceLoading: true,
      }),
    }),
    provideAppInitializer(() => {
      const language = inject(LanguageService);
      const auth = inject(AuthService);
      return language.initialize().then(() => auth.initialize());
    }),
    provideZoneChangeDetection({ eventCoalescing: true }),
    provideRouter(routes),
    provideHttpClient(
      withXhr(),
      withInterceptors([authInterceptor, languageInterceptor]),
    ),
  ],
};
