import { TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter,
  Router,
} from '@angular/router';
import { Subject } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { LoginComponent } from './login.component';

describe('LoginComponent', () => {
  let response: Subject<{ user: { role: string } }>;
  let login: ReturnType<typeof vi.fn>;
  beforeEach(() => {
    response = new Subject();
    login = vi.fn(() => response);
    TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { login } },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap({
                returnUrl: '//external.example',
              }),
            },
          },
        },
      ],
    });
  });

  it('marks invalid fields without calling the API', () => {
    const component = TestBed.createComponent(LoginComponent).componentInstance;
    component.submit();
    expect(component.form.controls.password.touched).toBe(true);
    expect(login).not.toHaveBeenCalled();
  });

  it('blocks duplicate submits and restores the form after failure', () => {
    const component = TestBed.createComponent(LoginComponent).componentInstance;
    component.form.setValue({
      phoneNumber: '0901234567',
      password: 'password123',
    });
    component.submit();
    component.submit();
    expect(login).toHaveBeenCalledTimes(1);
    expect(component.form.disabled).toBe(true);
    response.error({ status: 401 });
    expect(component.form.enabled).toBe(true);
    expect(component.loading).toBe(false);
    expect(component.error).not.toBe('');
  });

  it('rejects external return URLs and keeps the admin destination', () => {
    const navigate = vi
      .spyOn(TestBed.inject(Router), 'navigateByUrl')
      .mockResolvedValue(true);
    const component = TestBed.createComponent(LoginComponent).componentInstance;
    component.form.setValue({
      phoneNumber: '0901234567',
      password: 'password123',
    });
    component.submit();
    response.next({ user: { role: 'ADMIN' } });
    expect(navigate).toHaveBeenCalledWith('/admin');
  });
});
