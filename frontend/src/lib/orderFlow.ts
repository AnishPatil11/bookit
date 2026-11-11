import type { OrderStatus } from '../types';

/** The one forward step the kitchen can take from each status (mirrors OrderStatus.java). */
export const NEXT_STATUS: Record<OrderStatus, OrderStatus | null> = {
  PLACED: 'PREPARING',
  PREPARING: 'READY',
  READY: 'SERVED',
  SERVED: null,
  CANCELLED: null,
};

export const NEXT_LABEL: Record<OrderStatus, string> = {
  PLACED: 'Start preparing',
  PREPARING: 'Mark ready',
  READY: 'Mark served',
  SERVED: '',
  CANCELLED: '',
};
