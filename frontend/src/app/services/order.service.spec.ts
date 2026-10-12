import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { OrderService } from './order.service';

describe('VNPAY order API', () => {
  let service: OrderService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(OrderService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  it('loads payment capability without receiving a secret', () => {
    service
      .paymentMethods()
      .subscribe((value) => expect(value.vnpayEnabled).toBe(false));
    const request = http.expectOne('/api/v1/payments/methods');
    expect(request.request.method).toBe('GET');
    request.flush({ vnpayEnabled: false });
  });
  it('creates a link by persisted order ID and does not send a client amount', () => {
    service.createPaymentLink(7).subscribe();
    const request = http.expectOne('/api/v1/payments/vnpay/orders/7');
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({});
    request.flush({
      orderId: 7,
      paymentUrl: 'https://sandbox.vnpayment.vn/paymentv2/vpcpay.html',
      expiresAt: '2026-10-12T00:00:00Z',
    });
  });
  it('rejects redirect URLs outside the sandbox', () => {
    expect(() =>
      service.openSandboxPayment('https://attacker.example.com'),
    ).toThrow();
    expect(() => service.openSandboxPayment('javascript:alert(1)')).toThrow();
    expect(() =>
      service.openSandboxPayment(
        'https://sandbox.vnpayment.vn.attacker.example.com/paymentv2/vpcpay.html',
      ),
    ).toThrow();
  });
});
