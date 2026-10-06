export interface Voucher {
  id: number;
  code: string;
  type: 'FIXED' | 'PERCENT';
  amount: number;
  minimumSubtotal: number;
  maximumDiscount: number;
  startsAt: string;
  endsAt: string;
  usageLimit: number;
  perUserLimit: number;
  usedCount: number;
  targetUserId: number | null;
  active: boolean;
  version: number;
}
