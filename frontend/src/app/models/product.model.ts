import { Category } from './category.model';

export interface Product {
  id: number;
  name: string;
  price: number;
  thumbnail: string | null;
  description: string;
  category: Category;
  createdAt: string;
  updatedAt: string;
}
