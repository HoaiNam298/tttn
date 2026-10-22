import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { TranslatePipe } from '@ngx-translate/core';
import { LanguageService } from '../../services/language.service';

@Component({
  selector: 'app-language-selector',
  imports: [TranslatePipe],
  templateUrl: './language-selector.component.html',
  styleUrl: './language-selector.component.scss',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class LanguageSelectorComponent {
  readonly language = inject(LanguageService);
  change(event: Event): void {
    void this.language.setLanguage((event.target as HTMLSelectElement).value);
  }
}
