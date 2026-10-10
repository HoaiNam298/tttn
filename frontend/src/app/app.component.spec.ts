import { signal } from '@angular/core';
import { DeferBlockState, TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { AppComponent } from './app.component';
import { AuthService } from './services/auth.service';
import { NotificationService } from './services/notification.service';

describe('Deferred storefront shell', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            userId: signal(null),
            token: () => null,
            isAdmin: () => false,
          },
        },
        { provide: NotificationService, useValue: { unread: signal(0) } },
      ],
    });
  });
  it('renders the header after its deferred dependency loads', async () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const blocks = await fixture.getDeferBlocks();
    await blocks[0].render(DeferBlockState.Complete);
    fixture.detectChanges();
    expect(
      fixture.nativeElement.querySelector('app-store-header'),
    ).not.toBeNull();
    expect(fixture.nativeElement.textContent).toContain('Đăng nhập');
  });
  it('does not load the customer header on admin pages', () => {
    vi.spyOn(TestBed.inject(Router), 'url', 'get').mockReturnValue(
      '/admin/dashboard',
    );
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.site-header')).toBeNull();
    expect(fixture.nativeElement.querySelector('.site-footer')).toBeNull();
  });

  it('shows author identity and guest links in the storefront footer', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const footer: HTMLElement =
      fixture.nativeElement.querySelector('.site-footer');
    expect(footer.textContent).toContain('Nguyễn Hoài Nam');
    expect(footer.textContent).toContain('2431121074');
    expect(footer.querySelector('a[href="/login"]')).not.toBeNull();
    expect(footer.querySelector('a[href="/account"]')).toBeNull();
    expect(footer.querySelector('a[href="/orders"]')).toBeNull();
  });
});
