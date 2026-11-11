import { Link, NavLink, Outlet, useNavigate } from 'react-router-dom';
import { logout } from '../authSlice';
import { useAppDispatch, useAppSelector } from '../store';
import { Toaster } from './ui';

const navClass = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-1.5 text-sm font-medium ${isActive ? 'bg-pine-soft text-pine' : 'text-ink-soft hover:text-ink'}`;

export default function Layout() {
  const user = useAppSelector((s) => s.auth.user);
  const cartLines = useAppSelector((s) => s.cart.lines);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const itemCount = cartLines.reduce((n, l) => n + l.quantity, 0);

  return (
    <div className="flex min-h-screen flex-col">
      <header className="sticky top-0 z-40 border-b border-line bg-paper/95 backdrop-blur">
        <div className="mx-auto flex max-w-6xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3 sm:px-6">
          <Link to="/" className="flex items-center gap-2">
            <span className="flex h-8 w-8 items-center justify-center rounded-lg bg-pine">
              <span className="h-4 w-4 rounded-full border-2 border-paper" />
            </span>
            <span className="font-display text-xl font-bold">BookIt</span>
          </Link>
          <nav className="flex flex-1 flex-wrap items-center gap-1" aria-label="Main">
            <NavLink to="/" end className={navClass}>Restaurants</NavLink>
            {user && <NavLink to="/bookings" className={navClass}>My bookings</NavLink>}
            {user && <NavLink to="/orders" className={navClass}>My orders</NavLink>}
            {user?.role === 'ADMIN' && <NavLink to="/admin" className={navClass}>Admin</NavLink>}
          </nav>
          <div className="flex items-center gap-3 text-sm">
            {itemCount > 0 && (
              <span className="badge bg-turmeric-soft text-amber-800" title={`${itemCount} items in your cart`}>
                Cart: {itemCount}
              </span>
            )}
            {user ? (
              <>
                <span className="hidden text-ink-soft sm:inline">{user.name}</span>
                <button
                  className="btn-ghost btn-sm"
                  onClick={() => {
                    dispatch(logout());
                    navigate('/');
                  }}
                >
                  Sign out
                </button>
              </>
            ) : (
              <>
                <Link to="/login" className="btn-ghost btn-sm">Sign in</Link>
                <Link to="/register" className="btn-primary btn-sm">Create account</Link>
              </>
            )}
          </div>
        </div>
      </header>
      <main className="flex-1">
        <Outlet />
      </main>
      <footer className="border-t border-line py-6 text-center text-xs text-ink-soft">
        BookIt is a portfolio project built with Spring Boot, PostgreSQL and React.
      </footer>
      <Toaster />
    </div>
  );
}
