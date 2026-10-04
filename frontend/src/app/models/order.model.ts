export interface OrderItem {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  id: number;
  orderNumber: string;
  recipientName: string;
  phoneNumber: string;
  shippingAddress: string;
  note: string;
  status: string;
  paymentMethod: string;
  subtotal: number;
  shippingFee: number;
  total: number;
  createdAt: string;
  items: OrderItem[];
}
