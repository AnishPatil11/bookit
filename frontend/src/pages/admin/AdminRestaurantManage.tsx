import { useState, type FormEvent } from 'react';
import { Link, useParams } from 'react-router-dom';
import {
  useAdminMenuQuery, useAdminTablesQuery, useDeleteMenuItemMutation, useGetRestaurantQuery,
  useSaveMenuItemMutation, useSaveTableMutation,
} from '../../api';
import { ErrorNote, Spinner } from '../../components/ui';
import { errorMessage, money } from '../../lib/format';
import { useAppDispatch } from '../../store';
import { notify } from '../../uiSlice';
import type { MenuItem } from '../../types';

function TablesSection({ restaurantId }: { restaurantId: number }) {
  const { data, isLoading } = useAdminTablesQuery(restaurantId);
  const [save, { isLoading: saving, error }] = useSaveTableMutation();
  const [label, setLabel] = useState('');
  const [capacity, setCapacity] = useState(2);
  const dispatch = useAppDispatch();

  async function add(e: FormEvent) {
    e.preventDefault();
    try {
      await save({ restaurantId, body: { label, capacity } }).unwrap();
      setLabel('');
      dispatch(notify('success', 'Table added.'));
    } catch {
      /* shown below */
    }
  }

  return (
    <section className="card p-5">
      <h2 className="text-lg font-semibold">Tables</h2>
      <p className="text-sm text-ink-soft">Guests are seated at the smallest free table that fits their party.</p>
      {isLoading && <Spinner />}
      <ul className="mt-3 divide-y divide-line text-sm">
        {data?.map((t) => (
          <li key={t.id} className="flex items-center justify-between py-2">
            <span>
              <span className="font-medium">{t.label}</span> <span className="text-ink-soft">seats {t.capacity}</span>
              {!t.active && <span className="badge ml-2 bg-stone-100 text-ink-soft">disabled</span>}
            </span>
            <button
              className="btn-ghost btn-sm"
              onClick={() => save({ restaurantId, tableId: t.id, body: { label: t.label, capacity: t.capacity, active: !t.active } })}
            >
              {t.active ? 'Disable' : 'Enable'}
            </button>
          </li>
        ))}
      </ul>
      <form onSubmit={add} className="mt-4 flex flex-wrap items-end gap-3 border-t border-line pt-4">
        <div>
          <label className="label" htmlFor="t-label">Label</label>
          <input id="t-label" required maxLength={40} className="input w-32" placeholder="T7" value={label} onChange={(e) => setLabel(e.target.value)} />
        </div>
        <div>
          <label className="label" htmlFor="t-cap">Seats</label>
          <input id="t-cap" type="number" min={1} max={20} required className="input w-24" value={capacity} onChange={(e) => setCapacity(Number(e.target.value))} />
        </div>
        <button className="btn-primary" disabled={saving}>Add table</button>
      </form>
      {error && <div className="mt-3"><ErrorNote>{errorMessage(error)}</ErrorNote></div>}
    </section>
  );
}

interface MenuForm { id?: number; name: string; description: string; category: string; price: string; available: boolean }
const BLANK: MenuForm = { name: '', description: '', category: 'Mains', price: '', available: true };

function MenuSection({ restaurantId }: { restaurantId: number }) {
  const { data, isLoading } = useAdminMenuQuery(restaurantId);
  const [save, { isLoading: saving, error }] = useSaveMenuItemMutation();
  const [remove] = useDeleteMenuItemMutation();
  const [form, setForm] = useState<MenuForm>(BLANK);
  const dispatch = useAppDispatch();

  async function submit(e: FormEvent) {
    e.preventDefault();
    try {
      await save({
        restaurantId,
        itemId: form.id,
        body: { name: form.name, description: form.description, category: form.category, price: Number(form.price), available: form.available },
      }).unwrap();
      setForm(BLANK);
      dispatch(notify('success', form.id ? 'Dish updated.' : 'Dish added.'));
    } catch {
      /* shown below */
    }
  }

  const edit = (m: MenuItem) =>
    setForm({ id: m.id, name: m.name, description: m.description ?? '', category: m.category, price: String(m.price), available: m.available });

  async function del(m: MenuItem) {
    if (!window.confirm(`Delete "${m.name}"? Past orders keep their own copy of the name and price.`)) return;
    try {
      await remove({ restaurantId, itemId: m.id }).unwrap();
    } catch (e) {
      dispatch(notify('error', errorMessage(e)));
    }
  }

  return (
    <section className="card p-5">
      <h2 className="text-lg font-semibold">Menu</h2>
      {isLoading && <Spinner />}
      <ul className="mt-3 divide-y divide-line text-sm">
        {data?.map((m) => (
          <li key={m.id} className="flex items-center justify-between gap-3 py-2">
            <span>
              <span className="font-medium">{m.name}</span>{' '}
              <span className="text-ink-soft">{m.category} &middot; {money(m.price)}</span>
              {!m.available && <span className="badge ml-2 bg-stone-100 text-ink-soft">hidden</span>}
            </span>
            <span className="flex gap-2">
              <button className="btn-ghost btn-sm" onClick={() => edit(m)}>Edit</button>
              <button className="btn-danger btn-sm" onClick={() => del(m)}>Delete</button>
            </span>
          </li>
        ))}
      </ul>
      <form onSubmit={submit} className="mt-4 grid gap-3 border-t border-line pt-4 sm:grid-cols-2">
        <h3 className="font-medium sm:col-span-2">{form.id ? 'Edit dish' : 'Add a dish'}</h3>
        <div>
          <label className="label" htmlFor="m-name">Name</label>
          <input id="m-name" required maxLength={120} className="input" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
        </div>
        <div>
          <label className="label" htmlFor="m-cat">Category</label>
          <input id="m-cat" required maxLength={60} className="input" value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value })} />
        </div>
        <div>
          <label className="label" htmlFor="m-price">Price (INR)</label>
          <input id="m-price" type="number" min="0.01" step="0.01" required className="input" value={form.price} onChange={(e) => setForm({ ...form, price: e.target.value })} />
        </div>
        <label className="flex items-center gap-2 self-end pb-2 text-sm">
          <input type="checkbox" checked={form.available} onChange={(e) => setForm({ ...form, available: e.target.checked })} />
          Available to order
        </label>
        <div className="sm:col-span-2">
          <label className="label" htmlFor="m-desc">Description</label>
          <input id="m-desc" maxLength={500} className="input" value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />
        </div>
        {error && <div className="sm:col-span-2"><ErrorNote>{errorMessage(error)}</ErrorNote></div>}
        <div className="flex gap-2 sm:col-span-2">
          <button className="btn-primary" disabled={saving}>{saving ? 'Saving...' : form.id ? 'Save changes' : 'Add dish'}</button>
          {form.id && <button type="button" className="btn-ghost" onClick={() => setForm(BLANK)}>Cancel edit</button>}
        </div>
      </form>
    </section>
  );
}

export default function AdminRestaurantManage() {
  const id = Number(useParams().id);
  const { data: restaurant, isLoading, error } = useGetRestaurantQuery(id);
  if (isLoading) return <Spinner />;
  return (
    <div>
      <Link to="/admin/restaurants" className="text-sm text-ink-soft hover:text-ink">&larr; All restaurants</Link>
      <h2 className="mb-4 mt-2 text-2xl font-semibold">{restaurant?.name ?? `Restaurant #${id}`}</h2>
      {error && <p className="mb-4 text-sm text-ink-soft">Note: this restaurant is hidden from customers.</p>}
      <div className="grid gap-6 lg:grid-cols-2">
        <TablesSection restaurantId={id} />
        <MenuSection restaurantId={id} />
      </div>
    </div>
  );
}
