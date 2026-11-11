import { Navigate, Outlet, useLocation } from 'react-router-dom';
import { useAppSelector } from '../store';
import type { Role } from '../types';

/** Route guard. The server enforces the same rules; this only keeps the UI honest. */
export default function RequireAuth({ role }: { role?: Role }) {
  const user = useAppSelector((s) => s.auth.user);
  const location = useLocation();
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />;
  if (role && user.role !== role) return <Navigate to="/" replace />;
  return <Outlet />;
}
