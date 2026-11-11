import { useEffect, useState } from 'react';
import { useGetCuisinesQuery, useGetRestaurantsQuery } from '../api';
import RestaurantCard from '../components/RestaurantCard';
import { Empty, ErrorNote, Pagination, Spinner } from '../components/ui';
import { errorMessage } from '../lib/format';
import { useDebounce } from '../lib/useDebounce';

export default function Restaurants() {
  const [q, setQ] = useState('');
  const [city, setCity] = useState('');
  const [cuisine, setCuisine] = useState('');
  const [page, setPage] = useState(0);
  const dq = useDebounce(q);
  const dcity = useDebounce(city);

  useEffect(() => setPage(0), [dq, dcity, cuisine]);

  const { data: cuisines = [] } = useGetCuisinesQuery();
  const { data, isLoading, isFetching, error } = useGetRestaurantsQuery({ q: dq, city: dcity, cuisine, page });

  return (
    <div className="page">
      <section className="mb-8 max-w-2xl">
        <h1 className="text-4xl font-bold leading-tight sm:text-5xl">Your table is waiting.</h1>
        <p className="mt-3 text-ink-soft">
          Reserve a table in seconds and pre-order your meal, so the kitchen is ready when you walk in.
        </p>
      </section>

      <div className="card mb-6 grid gap-3 p-4 sm:grid-cols-3">
        <div>
          <label htmlFor="q" className="label">Search</label>
          <input id="q" className="input" placeholder="Restaurant or dish type" value={q} onChange={(e) => setQ(e.target.value)} />
        </div>
        <div>
          <label htmlFor="city" className="label">City</label>
          <input id="city" className="input" placeholder="Hubballi" value={city} onChange={(e) => setCity(e.target.value)} />
        </div>
        <div>
          <label htmlFor="cuisine" className="label">Cuisine</label>
          <select id="cuisine" className="input" value={cuisine} onChange={(e) => setCuisine(e.target.value)}>
            <option value="">All cuisines</option>
            {cuisines.map((c) => <option key={c} value={c}>{c}</option>)}
          </select>
        </div>
      </div>

      {error && <ErrorNote>{errorMessage(error)}</ErrorNote>}
      {isLoading ? (
        <Spinner label="Finding restaurants" />
      ) : data && data.content.length === 0 ? (
        <Empty title="No restaurants match">Try a different search or clear the filters.</Empty>
      ) : (
        <div className={`grid gap-5 sm:grid-cols-2 lg:grid-cols-3 ${isFetching ? 'opacity-70' : ''}`}>
          {data?.content.map((r) => <RestaurantCard key={r.id} r={r} />)}
        </div>
      )}
      {data && <Pagination page={data.page} totalPages={data.totalPages} onChange={setPage} />}
    </div>
  );
}
