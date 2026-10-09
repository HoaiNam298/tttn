import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { AuthService } from './auth.service';
import { NotificationService } from './notification.service';

describe('Notification authentication lifecycle', () => {
  const auth = { userId: signal<number | null>(null) };
  let service: NotificationService;
  let http: HttpTestingController;
  beforeEach(() => {
    auth.userId.set(null);
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AuthService, useValue: auth },
      ],
    });
    service = TestBed.inject(NotificationService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('does not poll for guests', async () => {
    TestBed.tick();
    await new Promise((resolve) => setTimeout(resolve, 5));
    http.expectNone('/api/v1/notifications/unread-count');
    expect(service.unread()).toBe(0);
  });

  it('clears unread state and stops requests on logout', async () => {
    auth.userId.set(7);
    TestBed.tick();
    await new Promise((resolve) => setTimeout(resolve, 5));
    http.expectOne('/api/v1/notifications/unread-count').flush(3);
    expect(service.unread()).toBe(3);
    auth.userId.set(null);
    TestBed.tick();
    expect(service.unread()).toBe(0);
    http.expectNone('/api/v1/notifications/unread-count');
  });
});
