import { TestBed } from '@angular/core/testing';
import {
  ActivatedRoute,
  convertToParamMap,
  provideRouter,
} from '@angular/router';
import { Subject } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
  let response: Subject<unknown>;
  let register: ReturnType<typeof vi.fn>;
  beforeEach(() => {
    response = new Subject();
    register = vi.fn(() => response);
    TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [
        provideRouter([]),
        { provide: AuthService, useValue: { register } },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap({}) } },
        },
      ],
    });
  });
  function validForm(component: RegisterComponent): void {
    component.form.setValue({
      fullName: ' Test User ',
      phoneNumber: '0901234567',
      address: ' Test address ',
      password: 'password123',
      confirmPassword: 'password123',
    });
  }

  it('rejects password mismatch without calling the API', () => {
    const component =
      TestBed.createComponent(RegisterComponent).componentInstance;
    validForm(component);
    component.form.controls.confirmPassword.setValue('different');
    component.submit();
    expect(register).not.toHaveBeenCalled();
    expect(component.error).not.toBe('');
  });

  it('trims payload and blocks duplicate submission while pending', () => {
    const component =
      TestBed.createComponent(RegisterComponent).componentInstance;
    validForm(component);
    component.submit();
    component.submit();
    expect(register).toHaveBeenCalledTimes(1);
    expect(register).toHaveBeenCalledWith({
      fullName: 'Test User',
      phoneNumber: '0901234567',
      address: 'Test address',
      password: 'password123',
    });
    expect(component.form.disabled).toBe(true);
    response.error({ status: 409 });
    expect(component.loading).toBe(false);
    expect(component.form.enabled).toBe(true);
    expect(component.error).toContain('đã được đăng ký');
  });
});
