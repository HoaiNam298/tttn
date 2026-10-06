export interface VoucherPayload {
  code: string;
  type: 'FIXED' | 'PERCENT';
  amount: number;
  minimumSubtotal: number;
  maximumDiscount: number;
  startsAt: string;
  endsAt: string;
  usageLimit: number;
  perUserLimit: number;
  targetUserId: number | null;
  active: boolean;
  version?: number;
}
