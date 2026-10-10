import { TestBed } from '@angular/core/testing';
import { MatDialog } from '@angular/material/dialog';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { vi } from 'vitest';
import { CatalogService } from '../../../services/catalog.service';
import { ProductReview } from '../../../models/product.model';
import { AdminReviewsComponent } from './admin-reviews.component';

describe('Material review management', () => {
  const review: ProductReview = {
    id: 2,
    reviewerName: 'Khách hàng',
    rating: 5,
    comment: 'Sản phẩm tốt',
    createdAt: '2026-10-10T00:00:00Z',
    productId: 1,
    productName: 'Điện thoại',
    shopReply: 'Cảm ơn',
    version: 4,
  };

  function setup() {
    const catalog = {
      adminReviews: vi.fn(() =>
        of({
          content: [review],
          number: 0,
          totalPages: 1,
          totalElements: 1,
        }),
      ),
      replyToReview: vi.fn(() => of(review)),
    };
    TestBed.configureTestingModule({
      imports: [AdminReviewsComponent],
      providers: [
        provideRouter([]),
        { provide: CatalogService, useValue: catalog },
      ],
    });
    const fixture = TestBed.createComponent(AdminReviewsComponent);
    fixture.detectChanges();
    return { fixture, component: fixture.componentInstance, catalog };
  }

  it('renders Material pagination and forwards the selected page size', () => {
    const { fixture, component, catalog } = setup();
    expect(fixture.nativeElement.textContent).toContain(review.comment);
    expect(fixture.nativeElement.querySelector('mat-paginator')).toBeTruthy();
    component.changePage({ pageIndex: 0, pageSize: 20, length: 1 });
    expect(catalog.adminReviews).toHaveBeenLastCalledWith(0, 20);
  });

  it('maps the existing reply and sends trimmed text with the original version', () => {
    const { component, catalog } = setup();
    component.open(review);
    expect(component.form.controls.reply.value).toBe('Cảm ơn');
    component.form.controls.reply.setValue('  Cảm ơn bạn  ');
    component.save();
    expect(catalog.replyToReview).toHaveBeenCalledWith(2, 'Cảm ơn bạn', 4);
    expect(component.selected).toBeUndefined();
  });

  it('rejects blank replies and duplicate saves while pending', () => {
    const { component, catalog } = setup();
    const pending = new Subject<ProductReview>();
    catalog.replyToReview.mockReturnValue(pending);
    component.open(review);
    component.form.controls.reply.setValue('   ');
    component.save();
    expect(catalog.replyToReview).not.toHaveBeenCalled();
    component.form.controls.reply.setValue('Cảm ơn');
    component.save();
    component.save();
    component.close();
    expect(catalog.replyToReview).toHaveBeenCalledTimes(1);
    expect(component.selected?.id).toBe(2);
    pending.next(review);
    pending.complete();
  });

  it('keeps the dialog open after a failure so the admin can retry', () => {
    const { component, catalog } = setup();
    catalog.replyToReview.mockReturnValue(
      throwError(() => new Error('conflict')),
    );
    component.open(review);
    component.save();
    expect(component.saving).toBe(false);
    expect(component.replyError).not.toBe('');
    expect(TestBed.inject(MatDialog).openDialogs).toHaveLength(1);
    component.close();
  });
});
