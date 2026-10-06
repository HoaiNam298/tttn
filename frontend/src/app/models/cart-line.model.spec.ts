import { Product } from './product.model';
import { toCartLine } from './cart-line.model';

describe('SKU cart pricing and availability', () => {
  const product: Product = {
    id: 8,
    name: 'Phone',
    price: 100,
    stock: 13,
    thumbnail: 'base.jpg',
    description: '',
    category: { id: 1, name: 'Phone' },
    createdAt: '',
    updatedAt: '',
    variants: [
      {
        id: 11,
        sku: 'BLACK',
        name: 'Black / 128GB',
        color: 'Black',
        size: '',
        capacity: '128GB',
        price: 200,
        stock: 3,
        imageUrl: 'black.jpg',
        active: true,
      },
    ],
  };
  it('uses SKU price, stock and image without mutating the catalog product', () => {
    const line = toCartLine(product, 2, 11);
    expect(line.lineTotal).toBe(400);
    expect(line.product.stock).toBe(3);
    expect(line.product.thumbnail).toBe('black.jpg');
    expect(line.variantName).toBe('Black / 128GB');
    expect(product.price).toBe(100);
  });
  it('blocks legacy/unavailable selections on products requiring a SKU', () => {
    expect(toCartLine(product, 1).product.stock).toBe(0);
    expect(toCartLine(product, 1, 999).product.stock).toBe(0);
    expect(
      toCartLine(
        { ...product, variants: [{ ...product.variants![0], active: false }] },
        1,
        11,
      ).product.stock,
    ).toBe(0);
  });
});
