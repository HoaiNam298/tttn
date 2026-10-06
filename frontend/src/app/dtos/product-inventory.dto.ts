export interface ProductVariantPayload {
  id?: number;
  sku: string;
  name: string;
  color: string;
  size: string;
  capacity: string;
  price: number;
  stock: number;
  imageUrl: string;
  active: boolean;
}

export interface ProductInventoryPayload {
  version: number;
  stock: number | null;
  variants: ProductVariantPayload[];
  images: string[];
}
