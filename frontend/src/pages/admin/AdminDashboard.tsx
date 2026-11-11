import { useGetStatsQuery } from '../../api';
import { ErrorNote, Spinner } from '../../components/ui';
import { errorMessage, money } from '../../lib/format';

function Tile({ label, value, hint }: { label: string; value: string | number; hint?: string }) {
  return (
    <div className="card p-5">
      <p className="text-xs font-medium uppercase tracking-wide text-ink-soft">{label}</p>
      <p className="mt-2 font-display text-3xl font-bold">{value}</p>
      {hint && <p className="mt-1 text-xs text-ink-faint">{hint}</p>}
    </div>
  );
}

export default function AdminDashboard() {
  const { data, isLoading, error } = useGetStatsQuery();
  if (isLoading) return <Spinner />;
  if (error || !data) return <ErrorNote>{errorMessage(error)}</ErrorNote>;
  return (
    <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
      <Tile label="Bookings today" value={data.bookingsToday} hint="Confirmed" />
      <Tile label="Upcoming bookings" value={data.upcomingBookings} hint="From now on" />
      <Tile label="Orders today" value={data.ordersToday} />
      <Tile label="Open orders" value={data.openOrders} hint="Placed, preparing or ready" />
      <Tile label="Revenue" value={money(data.revenue)} hint="All non-cancelled orders" />
      <Tile label="Restaurants" value={data.restaurants} />
    </div>
  );
}
