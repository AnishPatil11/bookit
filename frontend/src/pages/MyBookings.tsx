import { Link } from 'react-router-dom';
import { useCancelBookingMutation, useGetMyBookingsQuery } from '../api';
import { Empty, ErrorNote, Spinner, StatusBadge } from '../components/ui';
import { dateTime, errorMessage } from '../lib/format';
import { useAppDispatch } from '../store';
import { notify } from '../uiSlice';
import type { Booking } from '../types';

function BookingRow({ b }: { b: Booking }) {
  const [cancel, { isLoading }] = useCancelBookingMutation();
  const dispatch = useAppDispatch();

  async function onCancel() {
    if (!window.confirm('Cancel this booking? The table will be released.')) return;
    try {
      await cancel(b.id).unwrap();
      dispatch(notify('success', 'Booking cancelled.'));
    } catch (e) {
      dispatch(notify('error', errorMessage(e)));
    }
  }

  return (
    <li className="card flex flex-wrap items-center justify-between gap-4 p-4">
      <div>
        <Link to={`/restaurants/${b.restaurantId}`} className="font-semibold hover:underline">{b.restaurantName}</Link>
        <p className="mt-0.5 text-sm text-ink-soft">
          {dateTime(b.startsAt)} &middot; {b.partySize} {b.partySize === 1 ? 'guest' : 'guests'} &middot; table {b.tableLabel}
        </p>
        {b.notes && <p className="mt-1 text-xs text-ink-faint">Note: {b.notes}</p>}
      </div>
      <div className="flex items-center gap-3">
        <StatusBadge status={b.status} />
        {b.canCancel && (
          <button className="btn-danger btn-sm" disabled={isLoading} onClick={onCancel}>{isLoading ? 'Cancelling...' : 'Cancel'}</button>
        )}
      </div>
    </li>
  );
}

export default function MyBookings() {
  const { data, isLoading, error } = useGetMyBookingsQuery();
  const now = Date.now();
  const upcoming = (data ?? []).filter((b) => b.status === 'CONFIRMED' && new Date(b.endsAt).getTime() > now).reverse();
  const past = (data ?? []).filter((b) => !upcoming.includes(b));

  return (
    <div className="page max-w-3xl">
      <h1 className="text-3xl font-bold">My bookings</h1>
      {isLoading && <Spinner />}
      {error && <ErrorNote>{errorMessage(error)}</ErrorNote>}
      {data && data.length === 0 && (
        <div className="mt-6">
          <Empty title="No bookings yet">
            <Link to="/" className="text-pine underline">Find a restaurant</Link> and reserve your first table.
          </Empty>
        </div>
      )}
      {upcoming.length > 0 && (
        <>
          <h2 className="mb-3 mt-8 text-lg font-semibold">Upcoming</h2>
          <ul className="space-y-3">{upcoming.map((b) => <BookingRow key={b.id} b={b} />)}</ul>
        </>
      )}
      {past.length > 0 && (
        <>
          <h2 className="mb-3 mt-8 text-lg font-semibold">History</h2>
          <ul className="space-y-3">{past.map((b) => <BookingRow key={b.id} b={b} />)}</ul>
        </>
      )}
      <p className="mt-6 text-xs text-ink-faint">Bookings can be cancelled until 2 hours before they start.</p>
    </div>
  );
}
