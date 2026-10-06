export interface OrderItemPayload {
  productId: number;
  quantity: number;
  variantId?: number;
}

export interface CreateOrderPayload {
  expectedTotal?: number;
  shippingMethod?: 'STANDARD' | 'EXPRESS';
  voucherCode?: string;
  recipientName: string;
  phoneNumber: string;
  shippingAddress: string;
  note: string;
  items: OrderItemPayload[];
  requestId?: string;
}
