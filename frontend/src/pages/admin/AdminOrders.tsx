import { useState } from 'react';
import { useAdminOrdersQuery, useSetOrderStatusMutation } from '../../api';
import { Empty, ErrorNote, Pagination, Spinner, StatusBadge } from '../../components/ui';
import { dateTime, errorMessage, money } from '../../lib/format';
import { NEXT_LABEL, NEXT_STATUS } from '../../lib/orderFlow';
import { useAppDispatch } from '../../store';
import { notify } from '../../uiSlice';
import type { OrderStatus } from '../../types';

const FILTERS: (OrderStatus | '')[] = ['', 'PLACED', 'PREPARING', 'READY', 'SERVED', 'CANCELLED'];

export default function AdminOrders() {
  const [page, setPage] = useState(0);
  const [status, setStatusFilter] = useState<OrderStatus | ''>('');
  const { data, isLoading, error } = useAdminOrdersQuery({ status: status || undefined, page });
  const [setStatus, { isLoading: updating }] = useSetOrderStatusMutation();
  const dispatch = useAppDispatch();

  async function change(id: number, next: OrderStatus) {
    try {
      await setStatus({ id, status: next }).unwrap();
    } catch (e) {
      dispatch(notify('error', errorMessage(e)));
    }
  }

  return (
    <div>
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <h2 className="text-xl font-semibold">Orders</h2>
        <div className="flex flex-wrap gap-1" role="group" aria-label="Filter by status">
          {FILTERS.map((f) => (
            <button
              key={f || 'all'}
              aria-pressed={status === f}
              onClick={() => { setStatusFilter(f); setPage(0); }}
              className={`rounded-full border px-3 py-1 text-xs font-medium ${status === f ? 'border-pine bg-pine text-white' : 'border-line bg-white text-ink-soft hover:text-ink'}`}
            >
              {f ? f.toLowerCase() : 'all'}
            </button>
          ))}
        </div>
      </div>
      {isLoading && <Spinner />}
      {error && <ErrorNote>{errorMessage(error)}</ErrorNote>}
      {data && data.content.length === 0 && <Empty title="No orders found" />}
      <ul className="space-y-3">
        {data?.content.map((o) => {
          const next = NEXT_STATUS[o.status];
          return (
            <li key={o.id} className="card p-4">
              <div className="flex flex-wrap items-center justify-between gap-3">
                <div>
                  <p className="font-semibold">#{o.id} &middot; {o.restaurantName}</p>
                  <p className="text-xs text-ink-soft">{o.customerName} &middot; {dateTime(o.createdAt)}{o.bookingId ? ` · booking #${o.bookingId}` : ' · takeaway'}</p>
                </div>
                <div className="flex items-center gap-2">
                  <StatusBadge status={o.status} />
                  {next && <button className="btn-primary btn-sm" disabled={updating} onClick={() => change(o.id, next)}>{NEXT_LABEL[o.status]}</button>}
                  {o.canCancel && <button className="btn-danger btn-sm" disabled={updating} onClick={() => change(o.id, 'CANCELLED')}>Cancel</button>}
                </div>
              </div>
              <p className="mt-2 text-sm text-ink-soft">
                {o.items.map((i) => `${i.quantity} x ${i.name}`).join(', ')} &middot; <span className="font-semibold text-ink">{money(o.total)}</span>
              </p>
              {o.notes && <p className="mt-1 text-xs text-ink-faint">Note: {o.notes}</p>}
            </li>
          );
        })}
      </ul>
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  );
}
