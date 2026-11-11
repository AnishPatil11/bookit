import { useState, type FormEvent } from 'react';
import { Link } from 'react-router-dom';
import { useAdminRestaurantsQuery, useSaveRestaurantMutation } from '../../api';
import { ErrorNote, Spinner } from '../../components/ui';
import { clock, errorMessage } from '../../lib/format';
import { useAppDispatch } from '../../store';
import { notify } from '../../uiSlice';
import type { Restaurant, RestaurantInput } from '../../types';

const EMPTY: RestaurantInput = {
  name: '', description: '', cuisine: '', address: '', city: '', openTime: '11:00', closeTime: '22:00', imageUrl: '', active: true,
};

const toInput = (r: Restaurant): RestaurantInput => ({
  name: r.name, description: r.description ?? '', cuisine: r.cuisine, address: r.address ?? '', city: r.city,
  openTime: r.openTime.slice(0, 5), closeTime: r.closeTime.slice(0, 5), imageUrl: r.imageUrl ?? '', active: r.active,
});

export default function AdminRestaurants() {
  const { data, isLoading, error } = useAdminRestaurantsQuery();
  const [editing, setEditing] = useState<{ id?: number; form: RestaurantInput } | null>(null);
  const [save, { isLoading: saving, error: saveError }] = useSaveRestaurantMutation();
  const dispatch = useAppDispatch();

  async function submit(e: FormEvent) {
    e.preventDefault();
    if (!editing) return;
    try {
      await save({ id: editing.id, body: editing.form }).unwrap();
      dispatch(notify('success', editing.id ? 'Restaurant updated.' : 'Restaurant created. Now add tables and a menu.'));
      setEditing(null);
    } catch {
      /* error displayed in the form */
    }
  }

  const set = <K extends keyof RestaurantInput>(key: K, value: RestaurantInput[K]) =>
    setEditing((cur) => (cur ? { ...cur, form: { ...cur.form, [key]: value } } : cur));

  return (
    <div>
      <div className="mb-4 flex items-center justify-between">
        <h2 className="text-xl font-semibold">Restaurants</h2>
        <button className="btn-primary btn-sm" onClick={() => setEditing({ form: { ...EMPTY } })}>New restaurant</button>
      </div>

      {editing && (
        <form onSubmit={submit} className="card mb-6 grid gap-4 p-5 sm:grid-cols-2">
          <h3 className="text-lg font-semibold sm:col-span-2">{editing.id ? 'Edit restaurant' : 'New restaurant'}</h3>
          {saveError && <div className="sm:col-span-2"><ErrorNote>{errorMessage(saveError)}</ErrorNote></div>}
          <div>
            <label className="label" htmlFor="r-name">Name</label>
            <input id="r-name" required maxLength={120} className="input" value={editing.form.name} onChange={(e) => set('name', e.target.value)} />
          </div>
          <div>
            <label className="label" htmlFor="r-cuisine">Cuisine</label>
            <input id="r-cuisine" required maxLength={60} className="input" value={editing.form.cuisine} onChange={(e) => set('cuisine', e.target.value)} />
          </div>
          <div>
            <label className="label" htmlFor="r-city">City</label>
            <input id="r-city" required maxLength={80} className="input" value={editing.form.city} onChange={(e) => set('city', e.target.value)} />
          </div>
          <div>
            <label className="label" htmlFor="r-address">Address</label>
            <input id="r-address" maxLength={200} className="input" value={editing.form.address} onChange={(e) => set('address', e.target.value)} />
          </div>
          <div>
            <label className="label" htmlFor="r-open">Opens</label>
            <input id="r-open" type="time" required className="input" value={editing.form.openTime} onChange={(e) => set('openTime', e.target.value)} />
          </div>
          <div>
            <label className="label" htmlFor="r-close">Closes</label>
            <input id="r-close" type="time" required className="input" value={editing.form.closeTime} onChange={(e) => set('closeTime', e.target.value)} />
          </div>
          <div className="sm:col-span-2">
            <label className="label" htmlFor="r-image">Image URL</label>
            <input id="r-image" type="url" maxLength={500} className="input" placeholder="https://..." value={editing.form.imageUrl} onChange={(e) => set('imageUrl', e.target.value)} />
          </div>
          <div className="sm:col-span-2">
            <label className="label" htmlFor="r-desc">Description</label>
            <textarea id="r-desc" rows={3} maxLength={1000} className="input" value={editing.form.description} onChange={(e) => set('description', e.target.value)} />
          </div>
          <label className="flex items-center gap-2 text-sm sm:col-span-2">
            <input type="checkbox" checked={editing.form.active ?? true} onChange={(e) => set('active', e.target.checked)} />
            Visible to customers
          </label>
          <div className="flex gap-2 sm:col-span-2">
            <button className="btn-primary" disabled={saving}>{saving ? 'Saving...' : 'Save'}</button>
            <button type="button" className="btn-ghost" onClick={() => setEditing(null)}>Cancel</button>
          </div>
        </form>
      )}

      {isLoading && <Spinner />}
      {error && <ErrorNote>{errorMessage(error)}</ErrorNote>}
      <div className="card overflow-x-auto">
        <table className="w-full text-left text-sm">
          <thead className="border-b border-line text-xs uppercase tracking-wide text-ink-soft">
            <tr>
              <th className="px-4 py-3">Name</th><th className="px-4 py-3">City</th>
              <th className="px-4 py-3">Hours</th><th className="px-4 py-3">Status</th><th className="px-4 py-3" />
            </tr>
          </thead>
          <tbody className="divide-y divide-line">
            {data?.map((r) => (
              <tr key={r.id}>
                <td className="px-4 py-3 font-medium">{r.name}<span className="block text-xs font-normal text-ink-soft">{r.cuisine}</span></td>
                <td className="px-4 py-3">{r.city}</td>
                <td className="px-4 py-3">{clock(r.openTime)} to {clock(r.closeTime)}</td>
                <td className="px-4 py-3">
                  <span className={`badge ${r.active ? 'bg-pine-soft text-pine' : 'bg-stone-100 text-ink-soft'}`}>{r.active ? 'visible' : 'hidden'}</span>
                </td>
                <td className="whitespace-nowrap px-4 py-3 text-right">
                  <button className="btn-ghost btn-sm mr-2" onClick={() => setEditing({ id: r.id, form: toInput(r) })}>Edit</button>
                  <Link to={`/admin/restaurants/${r.id}`} className="btn-primary btn-sm">Tables and menu</Link>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}
