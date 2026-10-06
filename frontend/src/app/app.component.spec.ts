import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { AppComponent } from './app.component';
import { AuthService } from './services/auth.service';
import { signal } from '@angular/core';

describe('AppComponent', () => {
  const auth = {
    userId: signal<number | null>(null),
    token: vi.fn(),
    isAdmin: vi.fn(),
  };
  beforeEach(async () => {
    auth.userId.set(null);
    auth.token.mockReturnValue(null);
    auth.isAdmin.mockReturnValue(false);
    await TestBed.configureTestingModule({
      imports: [AppComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    }).compileComponents();
  });

  it('shows login/register but no account, orders or admin links for guests', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Đăng nhập');
    expect(text).toContain('Đăng ký');
    expect(text).not.toContain('Tài khoản');
    expect(text).not.toContain('Đơn hàng');
    expect(text).not.toContain('Quản trị');
  });

  it('shows account links for a customer without exposing the admin menu', () => {
    auth.userId.set(12);
    auth.token.mockReturnValue('valid-token');
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    const text = fixture.nativeElement.textContent;
    expect(text).toContain('Tài khoản');
    expect(text).toContain('Đơn hàng');
    expect(text).not.toContain('Quản trị');
    expect(text).not.toContain('Đăng nhập');
    expect(
      fixture.nativeElement.querySelector('a[href="/account/addresses"]'),
    ).toBeNull();
  });

  it('shows admin menu only for authenticated administrators', () => {
    auth.userId.set(1);
    auth.token.mockReturnValue('valid-token');
    auth.isAdmin.mockReturnValue(true);
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Quản trị');
    auth.token.mockReturnValue(null);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Quản trị');
    expect(fixture.nativeElement.textContent).toContain('Đăng nhập');
  });

  it('should create the application shell', () => {
    const fixture = TestBed.createComponent(AppComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.brand')
        ?.textContent,
    ).toContain('ShopApp');
  });
});
