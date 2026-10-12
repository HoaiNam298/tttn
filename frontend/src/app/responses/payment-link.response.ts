export interface PaymentLinkResponse {
  orderId: number;
  paymentUrl: string;
  expiresAt: string;
}

export interface PaymentMethodsResponse {
  vnpayEnabled: boolean;
}
