import { TranslateService, TranslatePipe } from '@ngx-translate/core';
import { LanguageSelectorComponent } from '../shared/language-selector.component';
import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  inject,
  signal,
  ViewChild,
} from '@angular/core';
import { BreakpointObserver } from '@angular/cdk/layout';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatButtonModule } from '@angular/material/button';
import { MatListModule } from '@angular/material/list';
import { MatMenuModule } from '@angular/material/menu';
import { MatSidenav, MatSidenavModule } from '@angular/material/sidenav';
import { MatToolbarModule } from '@angular/material/toolbar';
import { filter } from 'rxjs';
import {
  Router,
  RouterLink,
  RouterLinkActive,
  RouterOutlet,
  NavigationEnd,
} from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-admin',
  changeDetection: ChangeDetectionStrategy.Eager,
  imports: [
    TranslatePipe,
    LanguageSelectorComponent,
    RouterLink,
    RouterLinkActive,
    RouterOutlet,
    MatButtonModule,
    MatListModule,
    MatMenuModule,
    MatSidenavModule,
    MatToolbarModule,
  ],
  templateUrl: './admin.component.html',
  styleUrl: './admin.component.scss',
})
export class AdminComponent {
  private readonly translate = inject(TranslateService);
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);
  private readonly breakpoints = inject(BreakpointObserver);
  @ViewChild('drawer') private drawer?: MatSidenav;
  readonly compact = signal(this.breakpoints.isMatched('(max-width: 900px)'));
  readonly navigation = [
    {
      icon: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
      path: '/admin/dashboard',
      label: 'admin.overview',
    },
    {
      icon: 'm3 7 9-4 9 4v10l-9 4-9-4V7Zm0 0 9 4 9-4M12 11v10',
      path: '/admin/products',
      label: 'common.products',
    },
    {
      icon: 'M3 3h7v7H3z M14 3h7v7h-7z M3 14h7v7H3z M14 14h7v7h-7z',
      path: '/admin/categories',
      label: 'admin.navigationCategories',
    },
    {
      icon: 'M6 3h12v18l-3-2-3 2-3-2-3 2V3Zm3 5h6M9 12h6',
      path: '/admin/orders',
      label: 'footer.orders',
    },
    {
      icon: 'M3 5h18v5a2 2 0 0 0 0 4v5H3v-5a2 2 0 0 0 0-4V5Zm12 0v14',
      path: '/admin/vouchers',
      label: 'admin.vouchers',
    },
    {
      icon: 'M4 3h16v14H9l-5 4V3Zm4 5h8M8 12h5',
      path: '/admin/reviews',
      label: 'admin.navigationReviews',
    },
  ] as const;
  loggingOut = false;

  constructor() {
    this.breakpoints
      .observe('(max-width: 900px)')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(({ matches }) => this.compact.set(matches));
    this.router.events
      .pipe(
        filter((event) => event instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe(() => {
        if (this.compact()) {
          void this.drawer?.close();
        }
      });
  }

  get displayName(): string {
    return (
      this.auth.session()?.user?.fullName?.trim() ||
      this.translate.instant('admin.administrator')
    );
  }

  get initials(): string {
    return this.displayName
      .split(/\s+/)
      .slice(-2)
      .map((part) => part[0])
      .join('')
      .toUpperCase();
  }

  logout(): void {
    if (this.loggingOut) {
      return;
    }
    this.loggingOut = true;
    this.auth
      .logout()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          void this.router.navigate(['/login']);
        },
        error: () => {
          this.loggingOut = false;
        },
      });
  }
}
