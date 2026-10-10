import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { HeaderComponent } from './header.component';
import { AuthService } from '../../../services/auth.service';
import { signal } from '@angular/core';
import { NotificationService } from '../../../services/notification.service';
import { By } from '@angular/platform-browser';
import { MatMenuTrigger } from '@angular/material/menu';

describe('HeaderComponent', () => {
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
      imports: [HeaderComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: auth },
        { provide: NotificationService, useValue: { unread: signal(0) } },
      ],
    }).compileComponents();
  });

  it('shows login/register but no account, orders or admin links for guests', () => {
    const fixture = TestBed.createComponent(HeaderComponent);
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
    const fixture = TestBed.createComponent(HeaderComponent);
    fixture.detectChanges();
    fixture.debugElement
      .query(By.directive(MatMenuTrigger))
      .injector.get(MatMenuTrigger)
      .openMenu();
    fixture.detectChanges();
    const text = document.body.textContent;
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
    const fixture = TestBed.createComponent(HeaderComponent);
    fixture.detectChanges();
    const trigger = fixture.debugElement
      .query(By.directive(MatMenuTrigger))
      .injector.get(MatMenuTrigger);
    trigger.openMenu();
    fixture.detectChanges();
    expect(document.body.textContent).toContain('Quản trị');
    trigger.closeMenu();
    auth.token.mockReturnValue(null);
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).not.toContain('Quản trị');
    expect(fixture.nativeElement.textContent).toContain('Đăng nhập');
  });

  it('should create the storefront header', () => {
    const fixture = TestBed.createComponent(HeaderComponent);
    fixture.detectChanges();

    expect(fixture.componentInstance).toBeTruthy();
    expect(
      (fixture.nativeElement as HTMLElement).querySelector('.brand')
        ?.textContent,
    ).toContain('ShopApp');
  });

  it('trims search keywords and removes an empty search filter', () => {
    const navigate = vi
      .spyOn(TestBed.inject(Router), 'navigate')
      .mockResolvedValue(true);
    const component =
      TestBed.createComponent(HeaderComponent).componentInstance;
    component.searchForm.setValue({ keyword: '  phone  ' });
    component.search();
    expect(navigate).toHaveBeenCalledWith(['/products'], {
      queryParams: { keyword: 'phone' },
    });
    component.searchForm.setValue({ keyword: '  ' });
    component.search();
    expect(navigate).toHaveBeenLastCalledWith(['/products'], {
      queryParams: { keyword: null },
    });
  });
});
