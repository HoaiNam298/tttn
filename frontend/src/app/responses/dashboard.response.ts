import { DailyRevenue } from '../models/daily-revenue.model';
import { TopProduct } from '../models/top-product.model';
import { OrderSummary } from '../models/order.model';

export interface DashboardReport {
  from: string;
  to: string;
  timezone: string;
  revenue: number;
  completedOrders: number;
  estimatedCompletionCount: number;
  productCount: number;
  categoryCount: number;
  orderCount: number;
  statusCounts: Record<string, number>;
  dailyRevenue: DailyRevenue[];
  topProducts: TopProduct[];
  recentOrders: OrderSummary[];
}
