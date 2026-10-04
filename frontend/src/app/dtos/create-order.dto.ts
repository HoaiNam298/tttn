export interface OrderItemPayload {
  productId: number;
  quantity: number;
}

export interface CreateOrderPayload {
  recipientName: string;
  phoneNumber: string;
  shippingAddress: string;
  note: string;
  items: OrderItemPayload[];
}
