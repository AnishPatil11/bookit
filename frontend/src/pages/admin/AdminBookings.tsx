import { useState } from 'react';
import { useAdminBookingsQuery, useAdminRestaurantsQuery, useSetBookingStatusMutation } from '../../api';
import { Empty, ErrorNote, Pagination, Spinner, StatusBadge } from '../../components/ui';
import { dateTime, errorMessage } from '../../lib/format';
import { useAppDispatch } from '../../store';
import { notify } from '../../uiSlice';
import type { BookingStatus } from '../../types';

export default function AdminBookings() {
  const [page, setPage] = useState(0);
  const [restaurantId, setRestaurantId] = useState<number | undefined>(undefined);
  const { data: restaurants = [] } = useAdminRestaurantsQuery();
  const { data, isLoading, error } = useAdminBookingsQuery({ restaurantId, page });
  const [setStatus, { isLoading: updating }] = useSetBookingStatusMutation();
  const dispatch = useAppDispatch();

  async function change(id: number, status: BookingStatus) {
    try {
      await setStatus({ id, status }).unwrap();
    } catch (e) {
      dispatch(notify('error', errorMessage(e)));
    }
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-xl font-semibold">Bookings</h2>
        <select
          aria-label="Filter by restaurant" className="input w-auto"
          value={restaurantId ?? ''}
          onChange={(e) => { setRestaurantId(e.target.value ? Number(e.target.value) : undefined); setPage(0); }}
        >
          <option value="">All restaurants</option>
          {restaurants.map((r) => <option key={r.id} value={r.id}>{r.name}</option>)}
        </select>
      </div>
      {isLoading && <Spinner />}
      {error && <ErrorNote>{errorMessage(error)}</ErrorNote>}
      {data && data.content.length === 0 && <Empty title="No bookings found" />}
      {data && data.content.length > 0 && (
        <div className="card overflow-x-auto">
          <table className="w-full text-left text-sm">
            <thead className="border-b border-line text-xs uppercase tracking-wide text-ink-soft">
              <tr>
                <th className="px-4 py-3">When</th><th className="px-4 py-3">Restaurant</th><th className="px-4 py-3">Guest</th>
                <th className="px-4 py-3">Party</th><th className="px-4 py-3">Table</th><th className="px-4 py-3">Status</th><th className="px-4 py-3" />
              </tr>
            </thead>
            <tbody className="divide-y divide-line">
              {data.content.map((b) => (
                <tr key={b.id}>
                  <td className="whitespace-nowrap px-4 py-3">{dateTime(b.startsAt)}</td>
                  <td className="px-4 py-3">{b.restaurantName}</td>
                  <td className="px-4 py-3">{b.customerName}<span className="block text-xs text-ink-soft">{b.customerEmail}</span></td>
                  <td className="px-4 py-3">{b.partySize}</td>
                  <td className="px-4 py-3">{b.tableLabel}</td>
                  <td className="px-4 py-3"><StatusBadge status={b.status} /></td>
                  <td className="whitespace-nowrap px-4 py-3 text-right">
                    {b.status === 'CONFIRMED' && (
                      <span className="flex justify-end gap-2">
                        <button className="btn-ghost btn-sm" disabled={updating} onClick={() => change(b.id, 'COMPLETED')}>Completed</button>
                        <button className="btn-ghost btn-sm" disabled={updating} onClick={() => change(b.id, 'NO_SHOW')}>No-show</button>
                        <button className="btn-danger btn-sm" disabled={updating} onClick={() => change(b.id, 'CANCELLED')}>Cancel</button>
                      </span>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      )}
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  );
}
