export interface CheckoutQuote {
  subtotal: number;
  shippingMethod: 'STANDARD' | 'EXPRESS';
  shippingFee: number;
  voucherCode: string | null;
  discount: number;
  total: number;
}
