export type ProductSort = 'newest' | 'priceAsc' | 'priceDesc';

export interface ProductFilter {
  minPrice?: number;
  maxPrice?: number;
  sort?: ProductSort;
}

export const PRODUCT_SORT: Record<ProductSort, string> = {
  newest: 'createdAt,desc',
  priceAsc: 'price,asc',
  priceDesc: 'price,desc',
};
