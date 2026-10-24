export const ORDER_STATUSES = [
  'PENDING',
  'CONFIRMED',
  'SHIPPING',
  'DELIVERED',
  'COMPLETED',
  'CANCELLED',
] as const;

export type OrderStatus = (typeof ORDER_STATUSES)[number];

export const PAYMENT_STATUSES = [
  'UNPAID',
  'PENDING',
  'PAID',
  'FAILED',
  'EXPIRED',
  'CANCELLED',
  'REVIEW_REQUIRED',
] as const;

export type PaymentStatus = (typeof PAYMENT_STATUSES)[number];

export interface OrderItem {
  variantId?: number | null;
  variantName?: string | null;
  sku?: string | null;
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}

export interface Order {
  paymentStatus?: PaymentStatus;
  paymentExpiresAt?: string | null;
  shippingMethod?: 'STANDARD' | 'EXPRESS';
  voucherCode?: string | null;
  discount?: number;
  history?: OrderStatusHistory[];
  id: number;
  orderNumber: string;
  recipientName: string;
  phoneNumber: string;
  shippingAddress: string;
  note: string;
  status: OrderStatus;
  paymentMethod: string;
  subtotal: number;
  shippingFee: number;
  total: number;
  createdAt: string;
  items: OrderItem[];
}

export interface OrderStatusHistory {
  status: OrderStatus;
  actor: 'CUSTOMER' | 'ADMIN' | 'SYSTEM';
  occurredAt: string;
  imported: boolean;
}

export interface OrderSummary {
  paymentMethod?: string;
  paymentStatus?: PaymentStatus;
  id: number;
  orderNumber: string;
  recipientName: string;
  phoneNumber: string;
  status: OrderStatus;
  total: number;
  createdAt: string;
}
