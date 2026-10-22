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
    { path: '/admin/dashboard', label: 'admin.overview' },
    { path: '/admin/products', label: 'common.products' },
    { path: '/admin/categories', label: 'admin.navigationCategories' },
    { path: '/admin/orders', label: 'footer.orders' },
    { path: '/admin/vouchers', label: 'admin.vouchers' },
    { path: '/admin/reviews', label: 'admin.navigationReviews' },
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
