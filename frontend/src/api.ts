import { createApi, fetchBaseQuery } from '@reduxjs/toolkit/query/react';
import type { BaseQueryFn, FetchArgs, FetchBaseQueryError } from '@reduxjs/toolkit/query/react';
import { logout } from './authSlice';
import type { RootState } from './store';
import type {
  Availability, AuthResponse, Booking, BookingStatus, DiningTable, MenuItem, MenuItemInput, Order, OrderStatus,
  Page, Restaurant, RestaurantInput, Stats, TableInput, User,
} from './types';

const rawBaseQuery = fetchBaseQuery({
  baseUrl: `${import.meta.env.VITE_API_URL ?? ''}/api`,
  prepareHeaders: (headers, { getState }) => {
    const token = (getState() as RootState).auth.token;
    if (token) headers.set('Authorization', `Bearer ${token}`);
    return headers;
  },
});

/** Signs the user out automatically when the server says the token is no longer valid. */
const baseQuery: BaseQueryFn<string | FetchArgs, unknown, FetchBaseQueryError> = async (args, api, extra) => {
  const result = await rawBaseQuery(args, api, extra);
  const url = typeof args === 'string' ? args : args.url;
  if (result.error?.status === 401 && !url.startsWith('/auth/')) {
    api.dispatch(logout());
  }
  return result;
};

export interface RestaurantQuery { q?: string; city?: string; cuisine?: string; page?: number }
export interface AvailabilityQuery { id: number; date: string; partySize: number }
export interface CreateOrderInput {
  restaurantId: number;
  bookingId?: number;
  notes?: string;
  items: { menuItemId: number; quantity: number }[];
}

export const api = createApi({
  reducerPath: 'api',
  baseQuery,
  tagTypes: ['Restaurant', 'Menu', 'Table', 'Booking', 'Order', 'Availability', 'Admin'],
  endpoints: (b) => ({
    // ---------------------------------------------------------------- auth
    login: b.mutation<AuthResponse, { email: string; password: string }>({
      query: (body) => ({ url: '/auth/login', method: 'POST', body }),
    }),
    register: b.mutation<AuthResponse, { name: string; email: string; password: string }>({
      query: (body) => ({ url: '/auth/register', method: 'POST', body }),
    }),
    me: b.query<User, void>({ query: () => '/auth/me' }),

    // ---------------------------------------------------------------- browsing
    getRestaurants: b.query<Page<Restaurant>, RestaurantQuery>({
      query: (params) => ({ url: '/restaurants', params: { ...params, size: 9 } }),
      providesTags: ['Restaurant'],
    }),
    getCuisines: b.query<string[], void>({ query: () => '/restaurants/cuisines', providesTags: ['Restaurant'] }),
    getRestaurant: b.query<Restaurant, number>({
      query: (id) => `/restaurants/${id}`,
      providesTags: ['Restaurant'],
    }),
    getMenu: b.query<MenuItem[], number>({ query: (id) => `/restaurants/${id}/menu`, providesTags: ['Menu'] }),
    getAvailability: b.query<Availability, AvailabilityQuery>({
      query: ({ id, date, partySize }) => ({ url: `/restaurants/${id}/availability`, params: { date, partySize } }),
      providesTags: ['Availability'],
    }),

    // ---------------------------------------------------------------- bookings
    createBooking: b.mutation<Booking, { restaurantId: number; startsAt: string; partySize: number; notes?: string }>({
      query: (body) => ({ url: '/bookings', method: 'POST', body }),
      invalidatesTags: ['Booking', 'Availability', 'Admin'],
    }),
    getMyBookings: b.query<Booking[], void>({ query: () => '/bookings/my', providesTags: ['Booking'] }),
    cancelBooking: b.mutation<Booking, number>({
      query: (id) => ({ url: `/bookings/${id}/cancel`, method: 'POST' }),
      invalidatesTags: ['Booking', 'Availability', 'Admin'],
    }),

    // ---------------------------------------------------------------- orders
    createOrder: b.mutation<Order, CreateOrderInput>({
      query: (body) => ({ url: '/orders', method: 'POST', body }),
      invalidatesTags: ['Order', 'Admin'],
    }),
    getMyOrders: b.query<Order[], void>({ query: () => '/orders/my', providesTags: ['Order'] }),
    cancelOrder: b.mutation<Order, number>({
      query: (id) => ({ url: `/orders/${id}/cancel`, method: 'POST' }),
      invalidatesTags: ['Order', 'Admin'],
    }),

    // ---------------------------------------------------------------- admin
    getStats: b.query<Stats, void>({ query: () => '/admin/stats', providesTags: ['Admin'] }),
    adminRestaurants: b.query<Restaurant[], void>({ query: () => '/admin/restaurants', providesTags: ['Restaurant'] }),
    saveRestaurant: b.mutation<Restaurant, { id?: number; body: RestaurantInput }>({
      query: ({ id, body }) => ({
        url: id ? `/admin/restaurants/${id}` : '/admin/restaurants',
        method: id ? 'PUT' : 'POST',
        body,
      }),
      invalidatesTags: ['Restaurant', 'Admin'],
    }),
    adminTables: b.query<DiningTable[], number>({
      query: (id) => `/admin/restaurants/${id}/tables`,
      providesTags: ['Table'],
    }),
    saveTable: b.mutation<DiningTable, { restaurantId: number; tableId?: number; body: TableInput }>({
      query: ({ restaurantId, tableId, body }) => ({
        url: tableId ? `/admin/restaurants/${restaurantId}/tables/${tableId}` : `/admin/restaurants/${restaurantId}/tables`,
        method: tableId ? 'PUT' : 'POST',
        body,
      }),
      invalidatesTags: ['Table', 'Availability'],
    }),
    adminMenu: b.query<MenuItem[], number>({
      query: (id) => `/admin/restaurants/${id}/menu-items`,
      providesTags: ['Menu'],
    }),
    saveMenuItem: b.mutation<MenuItem, { restaurantId: number; itemId?: number; body: MenuItemInput }>({
      query: ({ restaurantId, itemId, body }) => ({
        url: itemId ? `/admin/restaurants/${restaurantId}/menu-items/${itemId}` : `/admin/restaurants/${restaurantId}/menu-items`,
        method: itemId ? 'PUT' : 'POST',
        body,
      }),
      invalidatesTags: ['Menu'],
    }),
    deleteMenuItem: b.mutation<void, { restaurantId: number; itemId: number }>({
      query: ({ restaurantId, itemId }) => ({ url: `/admin/restaurants/${restaurantId}/menu-items/${itemId}`, method: 'DELETE' }),
      invalidatesTags: ['Menu'],
    }),
    adminBookings: b.query<Page<Booking>, { restaurantId?: number; page: number }>({
      query: (params) => ({ url: '/admin/bookings', params: { ...params, size: 15 } }),
      providesTags: ['Booking', 'Admin'],
    }),
    setBookingStatus: b.mutation<Booking, { id: number; status: BookingStatus }>({
      query: ({ id, status }) => ({ url: `/admin/bookings/${id}/status`, method: 'POST', body: { status } }),
      invalidatesTags: ['Booking', 'Availability', 'Admin'],
    }),
    adminOrders: b.query<Page<Order>, { status?: OrderStatus; page: number }>({
      query: (params) => ({ url: '/admin/orders', params: { ...params, size: 15 } }),
      providesTags: ['Order', 'Admin'],
    }),
    setOrderStatus: b.mutation<Order, { id: number; status: OrderStatus }>({
      query: ({ id, status }) => ({ url: `/admin/orders/${id}/status`, method: 'POST', body: { status } }),
      invalidatesTags: ['Order', 'Admin'],
    }),
  }),
});

export const {
  useLoginMutation, useRegisterMutation, useMeQuery,
  useGetRestaurantsQuery, useGetCuisinesQuery, useGetRestaurantQuery, useGetMenuQuery, useGetAvailabilityQuery,
  useCreateBookingMutation, useGetMyBookingsQuery, useCancelBookingMutation,
  useCreateOrderMutation, useGetMyOrdersQuery, useCancelOrderMutation,
  useGetStatsQuery, useAdminRestaurantsQuery, useSaveRestaurantMutation,
  useAdminTablesQuery, useSaveTableMutation, useAdminMenuQuery, useSaveMenuItemMutation, useDeleteMenuItemMutation,
  useAdminBookingsQuery, useSetBookingStatusMutation, useAdminOrdersQuery, useSetOrderStatusMutation,
} = api;
