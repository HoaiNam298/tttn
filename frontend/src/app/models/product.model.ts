import { Category } from './category.model';
import { ProductVariant } from './product-variant.model';

export interface Product {
  id: number;
  name: string;
  price: number;
  thumbnail: string | null;
  description: string;
  stock?: number;
  images?: string[];
  variants?: ProductVariant[];
  version?: number;
  category: Category;
  createdAt: string;
  updatedAt: string;
}

export interface ProductReview {
  id: number;
  reviewerName: string;
  rating: number;
  comment: string;
  createdAt: string;
}

export interface ProductReviewOverview {
  averageRating: number;
  totalReviews: number;
  reviews: import('../responses/page.response').PageResponse<ProductReview>;
}
