import { useEffect } from 'react';
import { Navigate, Route, Routes } from 'react-router-dom';
import { useMeQuery } from './api';
import { setUser } from './authSlice';
import Layout from './components/Layout';
import RequireAuth from './components/RequireAuth';
import { useAppDispatch, useAppSelector } from './store';
import AdminBookings from './pages/admin/AdminBookings';
import AdminDashboard from './pages/admin/AdminDashboard';
import AdminLayout from './pages/admin/AdminLayout';
import AdminOrders from './pages/admin/AdminOrders';
import AdminRestaurantManage from './pages/admin/AdminRestaurantManage';
import AdminRestaurants from './pages/admin/AdminRestaurants';
import Login from './pages/Login';
import MyBookings from './pages/MyBookings';
import MyOrders from './pages/MyOrders';
import Register from './pages/Register';
import RestaurantDetail from './pages/RestaurantDetail';
import Restaurants from './pages/Restaurants';

/** Re-checks a stored token against the server once on load, refreshing the user (and signing out if stale). */
function SessionCheck() {
  const token = useAppSelector((s) => s.auth.token);
  const dispatch = useAppDispatch();
  const { data } = useMeQuery(undefined, { skip: !token });
  useEffect(() => {
    if (data) dispatch(setUser(data));
  }, [data, dispatch]);
  return null;
}

export default function App() {
  return (
    <>
      <SessionCheck />
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Restaurants />} />
          <Route path="restaurants/:id" element={<RestaurantDetail />} />
          <Route path="login" element={<Login />} />
          <Route path="register" element={<Register />} />

          <Route element={<RequireAuth />}>
            <Route path="bookings" element={<MyBookings />} />
            <Route path="orders" element={<MyOrders />} />
          </Route>

          <Route element={<RequireAuth role="ADMIN" />}>
            <Route path="admin" element={<AdminLayout />}>
              <Route index element={<AdminDashboard />} />
              <Route path="restaurants" element={<AdminRestaurants />} />
              <Route path="restaurants/:id" element={<AdminRestaurantManage />} />
              <Route path="bookings" element={<AdminBookings />} />
              <Route path="orders" element={<AdminOrders />} />
            </Route>
          </Route>

          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </>
  );
}
