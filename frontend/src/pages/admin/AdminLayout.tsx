import { NavLink, Outlet } from 'react-router-dom';

const tab = ({ isActive }: { isActive: boolean }) =>
  `border-b-2 px-1 pb-2 text-sm font-medium ${isActive ? 'border-pine text-pine' : 'border-transparent text-ink-soft hover:text-ink'}`;

export default function AdminLayout() {
  return (
    <div className="page">
      <h1 className="text-3xl font-bold">Admin</h1>
      <nav className="mt-4 flex gap-6 border-b border-line" aria-label="Admin sections">
        <NavLink to="/admin" end className={tab}>Dashboard</NavLink>
        <NavLink to="/admin/restaurants" className={tab}>Restaurants</NavLink>
        <NavLink to="/admin/bookings" className={tab}>Bookings</NavLink>
        <NavLink to="/admin/orders" className={tab}>Orders</NavLink>
      </nav>
      <div className="mt-6">
        <Outlet />
      </div>
    </div>
  );
}
