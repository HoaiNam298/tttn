import { CreateOrderPayload } from '../dtos/create-order.dto';

export interface CheckoutAttempt {
  source: string;
  payload: CreateOrderPayload;
}
