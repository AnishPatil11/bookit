import { useState } from 'react';
import { Link, useNavigate, useParams } from 'react-router-dom';
import {
  useCreateBookingMutation, useCreateOrderMutation, useGetAvailabilityQuery, useGetMenuQuery,
  useGetMyBookingsQuery, useGetRestaurantQuery,
} from '../api';
import { addItem, cartTotal, clearCart, setQuantity } from '../cartSlice';
import { Empty, ErrorNote, Spinner } from '../components/ui';
import { addDays, clock, dateTime, errorMessage, groupBy, isoDate, money, timeOnly } from '../lib/format';
import { useAppDispatch, useAppSelector } from '../store';
import { notify } from '../uiSlice';
import type { Restaurant } from '../types';

export default function RestaurantDetail() {
  const id = Number(useParams().id);
  const { data: restaurant, isLoading, error } = useGetRestaurantQuery(id, { skip: !Number.isFinite(id) });

  if (isLoading) return <Spinner />;
  if (error || !restaurant) {
    return (
      <div className="page">
        <ErrorNote>{error ? errorMessage(error) : 'Restaurant not found.'}</ErrorNote>
        <Link to="/" className="mt-4 inline-block text-sm text-pine underline">Back to restaurants</Link>
      </div>
    );
  }

  return (
    <div className="page">
      <Link to="/" className="text-sm text-ink-soft hover:text-ink">&larr; All restaurants</Link>
      <div className="mt-3 overflow-hidden rounded-2xl border border-line bg-white">
        {restaurant.imageUrl && <img src={restaurant.imageUrl} alt="" className="h-48 w-full object-cover sm:h-64" />}
        <div className="p-6">
          <div className="flex flex-wrap items-center gap-3">
            <h1 className="text-3xl font-bold">{restaurant.name}</h1>
            <span className="badge bg-turmeric-soft text-amber-800">{restaurant.cuisine}</span>
          </div>
          {restaurant.description && <p className="mt-2 max-w-2xl text-ink-soft">{restaurant.description}</p>}
          <p className="mt-3 text-sm text-ink-soft">
            {[restaurant.address, restaurant.city].filter(Boolean).join(', ')} &middot; Open {clock(restaurant.openTime)} to {clock(restaurant.closeTime)}
          </p>
        </div>
      </div>

      <div className="mt-8 grid gap-8 lg:grid-cols-[1fr_380px]">
        <MenuPanel restaurant={restaurant} />
        <div className="space-y-6 lg:sticky lg:top-24 lg:self-start">
          <BookingPanel restaurant={restaurant} />
          <CartPanel restaurant={restaurant} />
        </div>
      </div>
    </div>
  );
}

// ------------------------------------------------------------------ booking

function BookingPanel({ restaurant }: { restaurant: Restaurant }) {
  const user = useAppSelector((s) => s.auth.user);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const today = new Date();
  const [date, setDate] = useState(isoDate(today));
  const [partySize, setPartySize] = useState(2);
  const [slot, setSlot] = useState<string | null>(null);
  const [notes, setNotes] = useState('');

  const { data, isFetching, error } = useGetAvailabilityQuery({ id: restaurant.id, date, partySize });
  const [createBooking, { isLoading: booking, error: bookError }] = useCreateBookingMutation();

  async function confirm() {
    if (!slot) return;
    try {
      await createBooking({ restaurantId: restaurant.id, startsAt: slot, partySize, notes: notes || undefined }).unwrap();
      dispatch(notify('success', `Table booked for ${dateTime(slot)}.`));
      navigate('/bookings');
    } catch {
      setSlot(null); // availability has probably changed; the refetch shows the new slots
    }
  }

  return (
    <section className="card p-5" aria-labelledby="book-title">
      <h2 id="book-title" className="text-xl font-semibold">Reserve a table</h2>
      <div className="mt-4 grid grid-cols-2 gap-3">
        <div>
          <label htmlFor="date" className="label">Date</label>
          <input
            id="date" type="date" className="input" value={date}
            min={isoDate(today)} max={isoDate(addDays(today, 60))}
            onChange={(e) => { setDate(e.target.value); setSlot(null); }}
          />
        </div>
        <div>
          <label htmlFor="party" className="label">Guests</label>
          <select id="party" className="input" value={partySize} onChange={(e) => { setPartySize(Number(e.target.value)); setSlot(null); }}>
            {Array.from({ length: 12 }, (_, i) => i + 1).map((n) => <option key={n} value={n}>{n}</option>)}
          </select>
        </div>
      </div>

      <div className="mt-4">
        <p className="label">Time (table held for 90 minutes)</p>
        {error ? (
          <ErrorNote>{errorMessage(error)}</ErrorNote>
        ) : isFetching && !data ? (
          <Spinner label="Checking tables" />
        ) : data && data.slots.length === 0 ? (
          <p className="text-sm text-ink-soft">No more slots on this day. Try another date.</p>
        ) : (
          <div className={`grid grid-cols-3 gap-2 ${isFetching ? 'opacity-60' : ''}`}>
            {data?.slots.map((s) => (
              <button
                key={s.startsAt}
                type="button"
                disabled={!s.available}
                aria-pressed={slot === s.startsAt}
                onClick={() => setSlot(s.startsAt)}
                className={`rounded-lg border px-2 py-2 text-sm transition-colors ${
                  slot === s.startsAt
                    ? 'border-pine bg-pine text-white'
                    : s.available
                      ? 'border-line bg-white hover:border-pine'
                      : 'cursor-not-allowed border-line bg-stone-50 text-ink-faint line-through'
                }`}
              >
                {timeOnly(s.startsAt)}
              </button>
            ))}
          </div>
        )}
      </div>

      {slot && (
        <div className="mt-4">
          <label htmlFor="notes" className="label">Note for the restaurant (optional)</label>
          <input id="notes" className="input" maxLength={300} placeholder="Birthday, high chair, window seat..." value={notes} onChange={(e) => setNotes(e.target.value)} />
        </div>
      )}
      {bookError && <div className="mt-3"><ErrorNote>{errorMessage(bookError)}</ErrorNote></div>}

      {user ? (
        <button className="btn-primary mt-4 w-full" disabled={!slot || booking} onClick={confirm}>
          {booking ? 'Booking...' : slot ? `Confirm ${timeOnly(slot)} for ${partySize}` : 'Pick a time'}
        </button>
      ) : (
        <Link to="/login" state={{ from: `/restaurants/${restaurant.id}` }} className="btn-primary mt-4 w-full">
          Sign in to book
        </Link>
      )}
    </section>
  );
}

// ------------------------------------------------------------------ menu

function MenuPanel({ restaurant }: { restaurant: Restaurant }) {
  const { data: menu, isLoading, error } = useGetMenuQuery(restaurant.id);
  const dispatch = useAppDispatch();

  if (isLoading) return <Spinner label="Loading menu" />;
  if (error) return <ErrorNote>{errorMessage(error)}</ErrorNote>;
  if (!menu || menu.length === 0) return <Empty title="Menu coming soon" />;

  const groups = groupBy(menu, (m) => m.category);
  return (
    <section aria-labelledby="menu-title">
      <h2 id="menu-title" className="text-2xl font-semibold">Menu</h2>
      <p className="mt-1 text-sm text-ink-soft">Add dishes to your cart to pre-order. Prices are in rupees.</p>
      {Object.entries(groups).map(([category, items]) => (
        <div key={category} className="mt-6">
          <h3 className="mb-2 text-sm font-semibold uppercase tracking-wide text-pine">{category}</h3>
          <ul className="card divide-y divide-line">
            {items.map((m) => (
              <li key={m.id} className="flex items-start justify-between gap-4 p-4">
                <div>
                  <p className="font-medium">{m.name}</p>
                  {m.description && <p className="mt-0.5 text-sm text-ink-soft">{m.description}</p>}
                  <p className="mt-1 text-sm font-semibold">{money(m.price)}</p>
                </div>
                <button
                  className="btn-ghost btn-sm shrink-0"
                  onClick={() =>
                    dispatch(addItem({
                      restaurantId: restaurant.id,
                      restaurantName: restaurant.name,
                      item: { menuItemId: m.id, name: m.name, price: m.price },
                    }))
                  }
                >
                  Add
                </button>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </section>
  );
}

// ------------------------------------------------------------------ cart

function CartPanel({ restaurant }: { restaurant: Restaurant }) {
  const cart = useAppSelector((s) => s.cart);
  const user = useAppSelector((s) => s.auth.user);
  const dispatch = useAppDispatch();
  const navigate = useNavigate();
  const [notes, setNotes] = useState('');
  const [bookingId, setBookingId] = useState('');
  const [createOrder, { isLoading, error }] = useCreateOrderMutation();
  const { data: bookings = [] } = useGetMyBookingsQuery(undefined, { skip: !user });

  if (cart.restaurantId !== restaurant.id || cart.lines.length === 0) {
    return (
      <section className="card p-5">
        <h2 className="text-xl font-semibold">Your order</h2>
        <p className="mt-2 text-sm text-ink-soft">Your cart is empty. Add dishes from the menu to pre-order.</p>
      </section>
    );
  }

  const now = Date.now();
  const attachable = bookings.filter(
    (b) => b.restaurantId === restaurant.id && b.status === 'CONFIRMED' && new Date(b.endsAt).getTime() > now,
  );

  async function place() {
    try {
      await createOrder({
        restaurantId: restaurant.id,
        bookingId: bookingId ? Number(bookingId) : undefined,
        notes: notes || undefined,
        items: cart.lines.map((l) => ({ menuItemId: l.menuItemId, quantity: l.quantity })),
      }).unwrap();
      dispatch(clearCart());
      dispatch(notify('success', 'Order placed. The kitchen has it.'));
      navigate('/orders');
    } catch {
      /* error rendered below */
    }
  }

  return (
    <section className="card p-5" aria-labelledby="cart-title">
      <h2 id="cart-title" className="text-xl font-semibold">Your order</h2>
      <ul className="mt-3 divide-y divide-line">
        {cart.lines.map((l) => (
          <li key={l.menuItemId} className="flex items-center justify-between gap-3 py-2 text-sm">
            <span className="min-w-0 flex-1 truncate">{l.name}</span>
            <span className="flex items-center gap-1">
              <button aria-label={`Remove one ${l.name}`} className="btn-ghost btn-sm !px-2" onClick={() => dispatch(setQuantity({ menuItemId: l.menuItemId, quantity: l.quantity - 1 }))}>-</button>
              <span className="w-6 text-center">{l.quantity}</span>
              <button aria-label={`Add one ${l.name}`} className="btn-ghost btn-sm !px-2" onClick={() => dispatch(setQuantity({ menuItemId: l.menuItemId, quantity: l.quantity + 1 }))}>+</button>
            </span>
            <span className="w-20 text-right font-medium">{money(l.price * l.quantity)}</span>
          </li>
        ))}
      </ul>
      <p className="mt-3 flex justify-between border-t border-line pt-3 font-semibold">
        <span>Total</span>
        <span>{money(cartTotal(cart.lines))}</span>
      </p>

      {user && attachable.length > 0 && (
        <div className="mt-4">
          <label htmlFor="booking" className="label">Serve at my booking</label>
          <select id="booking" className="input" value={bookingId} onChange={(e) => setBookingId(e.target.value)}>
            <option value="">No booking (takeaway)</option>
            {attachable.map((b) => <option key={b.id} value={b.id}>{dateTime(b.startsAt)} - table {b.tableLabel}</option>)}
          </select>
        </div>
      )}
      <div className="mt-4">
        <label htmlFor="order-notes" className="label">Kitchen note (optional)</label>
        <input id="order-notes" className="input" maxLength={300} placeholder="Less spicy, no onion..." value={notes} onChange={(e) => setNotes(e.target.value)} />
      </div>
      {error && <div className="mt-3"><ErrorNote>{errorMessage(error)}</ErrorNote></div>}

      {user ? (
        <button className="btn-accent mt-4 w-full" disabled={isLoading} onClick={place}>
          {isLoading ? 'Placing...' : `Place order - ${money(cartTotal(cart.lines))}`}
        </button>
      ) : (
        <Link to="/login" state={{ from: `/restaurants/${restaurant.id}` }} className="btn-accent mt-4 w-full">Sign in to order</Link>
      )}
      <button className="mt-2 w-full text-center text-xs text-ink-soft underline" onClick={() => dispatch(clearCart())}>Clear cart</button>
    </section>
  );
}
