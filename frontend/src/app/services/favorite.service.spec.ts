import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FavoriteService } from './favorite.service';

describe('Favorites API contract', () => {
  let service: FavoriteService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(FavoriteService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('uses idempotent PUT to add without client user ID', () => {
    service.set(8, true).subscribe();
    const request = http.expectOne('/api/v1/favorites/8');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({});
    request.flush(null);
  });

  it('uses DELETE to remove instead of a retry-unsafe toggle', () => {
    service.set(8, false).subscribe();
    const request = http.expectOne('/api/v1/favorites/8');
    expect(request.request.method).toBe('DELETE');
    request.flush(null);
  });

  it('paginates favorites', () => {
    service.list(2).subscribe();
    const request = http.expectOne(
      (value) => value.url === '/api/v1/favorites',
    );
    expect(request.request.params.get('page')).toBe('2');
    expect(request.request.params.get('size')).toBe('12');
    request.flush({ content: [] });
  });
});
