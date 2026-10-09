import { provideHttpClient } from '@angular/common/http';
import {
  HttpTestingController,
  provideHttpClientTesting,
} from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { CatalogService } from './catalog.service';

describe('Review API contract', () => {
  let service: CatalogService;
  let http: HttpTestingController;
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()],
    });
    service = TestBed.inject(CatalogService);
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('sends rating filter to the server before pagination', () => {
    service.reviews(8, 1, 4).subscribe();
    const request = http.expectOne(
      (value) => value.url === '/api/v1/products/8/reviews',
    );
    expect(request.request.params.get('rating')).toBe('4');
    expect(request.request.params.get('page')).toBe('1');
    request.flush({ reviews: { content: [] } });
  });

  it('uploads files and JSON metadata as multipart without forcing Content-Type', () => {
    const file = new File(['photo'], 'photo.png', { type: 'image/png' });
    service.createReview(8, 21, 5, 'Great', 3, [file]).subscribe();
    const request = http.expectOne('/api/v1/products/8/reviews');
    expect(request.request.body instanceof FormData).toBe(true);
    expect(request.request.body.getAll('images').length).toBe(1);
    expect(request.request.body.get('review').type).toBe('application/json');
    expect(request.request.headers.has('Content-Type')).toBe(false);
    request.flush({ id: 1 });
  });

  it('keeps the existing JSON API for reviews without photos', () => {
    service.createReview(8, 21, 5, 'Great').subscribe();
    const request = http.expectOne('/api/v1/products/8/reviews');
    expect(request.request.body).toEqual({
      orderId: 21,
      rating: 5,
      comment: 'Great',
    });
    request.flush({ id: 1 });
  });

  it('sends review version when replying', () => {
    service.replyToReview(3, 'Thank you', 2).subscribe();
    const request = http.expectOne('/api/v1/admin/reviews/3/reply');
    expect(request.request.method).toBe('PUT');
    expect(request.request.body).toEqual({ reply: 'Thank you', version: 2 });
    request.flush({ id: 3 });
  });
});
