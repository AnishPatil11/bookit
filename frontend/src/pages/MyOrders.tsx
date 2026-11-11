import { Link } from 'react-router-dom';
import { useCancelOrderMutation, useGetMyOrdersQuery } from '../api';
import { Empty, ErrorNote, Spinner, StatusBadge } from '../components/ui';
import { dateTime, errorMessage, money } from '../lib/format';
import { useAppDispatch } from '../store';
import { notify } from '../uiSlice';
import type { Order } from '../types';

function OrderCard({ o }: { o: Order }) {
  const [cancel, { isLoading }] = useCancelOrderMutation();
  const dispatch = useAppDispatch();

  async function onCancel() {
    if (!window.confirm('Cancel this order?')) return;
    try {
      await cancel(o.id).unwrap();
      dispatch(notify('success', 'Order cancelled.'));
    } catch (e) {
      dispatch(notify('error', errorMessage(e)));
    }
  }

  return (
    <li className="card p-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <Link to={`/restaurants/${o.restaurantId}`} className="font-semibold hover:underline">{o.restaurantName}</Link>
          <p className="text-xs text-ink-soft">Order #{o.id} &middot; {dateTime(o.createdAt)}</p>
        </div>
        <div className="flex items-center gap-3">
          <StatusBadge status={o.status} />
          {o.canCancel && <button className="btn-danger btn-sm" disabled={isLoading} onClick={onCancel}>Cancel</button>}
        </div>
      </div>
      <ul className="mt-3 divide-y divide-line text-sm">
        {o.items.map((i, idx) => (
          <li key={idx} className="flex justify-between py-1.5">
            <span>{i.quantity} x {i.name}</span>
            <span>{money(i.lineTotal)}</span>
          </li>
        ))}
      </ul>
      <p className="mt-2 flex justify-between border-t border-line pt-2 text-sm font-semibold">
        <span>Total</span><span>{money(o.total)}</span>
      </p>
      {o.notes && <p className="mt-1 text-xs text-ink-faint">Note: {o.notes}</p>}
    </li>
  );
}

export default function MyOrders() {
  const { data, isLoading, error } = useGetMyOrdersQuery();
  return (
    <div className="page max-w-3xl">
      <h1 className="text-3xl font-bold">My orders</h1>
      {isLoading && <Spinner />}
      {error && <ErrorNote>{errorMessage(error)}</ErrorNote>}
      {data && data.length === 0 && (
        <div className="mt-6">
          <Empty title="No orders yet">Open a restaurant and add dishes to your cart.</Empty>
        </div>
      )}
      <ul className="mt-6 space-y-4">{data?.map((o) => <OrderCard key={o.id} o={o} />)}</ul>
    </div>
  );
}
