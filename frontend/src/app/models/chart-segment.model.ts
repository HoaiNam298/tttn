import { OrderStatus } from './order.model';

export interface ChartSegment {
  status: OrderStatus;
  label: string;
  count: number;
  percentage: number;
  color: string;
  dashArray: string;
  dashOffset: number;
}
