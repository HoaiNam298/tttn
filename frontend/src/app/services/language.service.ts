import { DOCUMENT } from '@angular/common';
import { DestroyRef, Injectable, inject, signal } from '@angular/core';
import { TranslateService } from '@ngx-translate/core';
import { firstValueFrom } from 'rxjs';
import vietnamese from '../../../public/i18n/vi.json';

@Injectable({ providedIn: 'root' })
export class LanguageService {
  private readonly translate = inject(TranslateService);
  private readonly document = inject(DOCUMENT);
  private readonly selected = signal<'vi' | 'en'>('vi');
  readonly current = this.selected.asReadonly();
  private revision = 0;
  private readonly window = this.document.defaultView;

  constructor() {
    // Keep Vietnamese available even when loading assets fails.
    this.translate.setTranslation('vi', vietnamese);
    this.document.documentElement.lang = 'vi';
    const synchronize = (event: StorageEvent) => {
      if (event.key !== 'shopapp.language' && event.key !== null) {
        return;
      }
      try {
        if (event.storageArea !== this.window?.localStorage) {
          return;
        }
      } catch {
        return;
      }
      void this.setLanguage(event.newValue, false);
    };
    this.window?.addEventListener('storage', synchronize);
    inject(DestroyRef).onDestroy(() =>
      this.window?.removeEventListener('storage', synchronize),
    );
  }

  async initialize(): Promise<void> {
    let saved: string | null = null;
    try {
      saved = this.window?.localStorage.getItem('shopapp.language') ?? null;
    } catch {
      /* Storage may be blocked. */
    }
    await this.setLanguage(saved, false);
  }

  async setLanguage(value: string | null, persist = true): Promise<void> {
    const language = value === 'en' ? 'en' : 'vi';
    const revision = ++this.revision;
    try {
      await firstValueFrom(this.translate.use(language));
    } catch {
      if (revision !== this.revision) {
        return;
      }
      await firstValueFrom(this.translate.use('vi'));
      if (revision !== this.revision) {
        return;
      }
      this.apply('vi', persist);
      return;
    }
    if (revision === this.revision) {
      this.apply(language, persist);
    }
  }

  private apply(language: 'vi' | 'en', persist: boolean): void {
    this.selected.set(language);
    this.document.documentElement.lang = language;
    if (persist) {
      try {
        this.window?.localStorage.setItem('shopapp.language', language);
      } catch {
        /* Keep the in-memory selection. */
      }
    }
  }
}
