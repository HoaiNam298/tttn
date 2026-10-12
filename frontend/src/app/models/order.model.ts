export type OrderStatus =
  | 'PENDING'
  | 'CONFIRMED'
  | 'SHIPPING'
  | 'DELIVERED'
  | 'COMPLETED'
  | 'CANCELLED';

export type PaymentStatus =
  | 'UNPAID'
  | 'PENDING'
  | 'PAID'
  | 'FAILED'
  | 'EXPIRED'
  | 'CANCELLED'
  | 'REVIEW_REQUIRED';

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  UNPAID: 'Chưa thanh toán',
  PENDING: 'Chờ thanh toán',
  PAID: 'Đã thanh toán',
  FAILED: 'Thanh toán thất bại',
  EXPIRED: 'Hết hạn thanh toán',
  CANCELLED: 'Đã hủy thanh toán',
  REVIEW_REQUIRED: 'Đã nhận thông báo tiền — cần kiểm tra',
};

export const ORDER_STATUS_LABELS: Record<OrderStatus, string> = {
  PENDING: 'Chờ xác nhận',
  CONFIRMED: 'Chờ vận chuyển',
  SHIPPING: 'Đang vận chuyển',
  DELIVERED: 'Đã giao · Chờ nhận hàng',
  COMPLETED: 'Hoàn thành',
  CANCELLED: 'Đã hủy',
};

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
