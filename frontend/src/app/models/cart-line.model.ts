import { Product } from './product.model';

export interface CartLine {
  variantId?: number;
  variantName?: string;
  product: Product;
  quantity: number;
  lineTotal: number;
}

export function toCartLine(
  product: Product,
  quantity: number,
  variantId?: number,
): CartLine {
  const variant = product.variants?.find((item) => item.id === variantId);
  const needsVariant = (product.variants?.length ?? 0) > 0;
  const available = variantId != null ? !!variant?.active : !needsVariant;
  const pricedProduct = {
    ...product,
    price: variant?.price ?? product.price,
    stock: available ? (variant?.stock ?? product.stock ?? 0) : 0,
    thumbnail: variant?.imageUrl || product.thumbnail,
  };
  return {
    product: pricedProduct,
    quantity,
    lineTotal: pricedProduct.price * quantity,
    ...(variantId != null
      ? {
          variantId,
          variantName: variant?.name ?? 'Phân loại không còn khả dụng',
        }
      : needsVariant
        ? { variantName: 'Cần chọn lại phân loại sản phẩm' }
        : {}),
  };
}
