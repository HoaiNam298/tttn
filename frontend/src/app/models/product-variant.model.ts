export interface ProductVariant {
  id: number;
  sku: string;
  name: string;
  color: string;
  size: string;
  capacity: string;
  price: number;
  stock: number;
  imageUrl: string | null;
  active: boolean;
}
