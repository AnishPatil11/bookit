export type Role = 'CUSTOMER' | 'ADMIN';
export type BookingStatus = 'CONFIRMED' | 'CANCELLED' | 'COMPLETED' | 'NO_SHOW';
export type OrderStatus = 'PLACED' | 'PREPARING' | 'READY' | 'SERVED' | 'CANCELLED';

export interface User {
  id: number;
  name: string;
  email: string;
  role: Role;
}
export interface AuthResponse {
  token: string;
  user: User;
}

export interface Restaurant {
  id: number;
  name: string;
  description: string | null;
  cuisine: string;
  address: string | null;
  city: string;
  openTime: string; // "11:00:00"
  closeTime: string;
  imageUrl: string | null;
  active: boolean;
}
export interface RestaurantInput {
  name: string;
  description?: string;
  cuisine: string;
  address?: string;
  city: string;
  openTime: string; // "HH:mm"
  closeTime: string;
  imageUrl?: string;
  active?: boolean;
}

export interface MenuItem {
  id: number;
  name: string;
  description: string | null;
  category: string;
  price: number;
  available: boolean;
}
export interface MenuItemInput {
  name: string;
  description?: string;
  category: string;
  price: number;
  available?: boolean;
}

export interface DiningTable {
  id: number;
  label: string;
  capacity: number;
  active: boolean;
}
export interface TableInput {
  label: string;
  capacity: number;
  active?: boolean;
}

export interface Slot {
  startsAt: string; // "2026-10-26T19:00:00"
  endsAt: string;
  available: boolean;
}
export interface Availability {
  date: string;
  partySize: number;
  slots: Slot[];
}

export interface Booking {
  id: number;
  restaurantId: number;
  restaurantName: string;
  tableLabel: string;
  customerName: string;
  customerEmail: string;
  startsAt: string;
  endsAt: string;
  partySize: number;
  status: BookingStatus;
  notes: string | null;
  canCancel: boolean;
}

export interface OrderItem {
  name: string;
  unitPrice: number;
  quantity: number;
  lineTotal: number;
}
export interface Order {
  id: number;
  restaurantId: number;
  restaurantName: string;
  bookingId: number | null;
  customerName: string;
  status: OrderStatus;
  total: number;
  notes: string | null;
  createdAt: string;
  items: OrderItem[];
  canCancel: boolean;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Stats {
  restaurants: number;
  bookingsToday: number;
  upcomingBookings: number;
  ordersToday: number;
  openOrders: number;
  revenue: number;
}
