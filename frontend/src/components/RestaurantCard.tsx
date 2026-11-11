import { Link } from 'react-router-dom';
import { clock } from '../lib/format';
import type { Restaurant } from '../types';

export default function RestaurantCard({ r }: { r: Restaurant }) {
  return (
    <Link to={`/restaurants/${r.id}`} className="card group flex flex-col overflow-hidden transition-shadow hover:shadow-md">
      <div className="aspect-[16/10] overflow-hidden bg-pine-soft">
        {r.imageUrl ? (
          <img src={r.imageUrl} alt="" loading="lazy" className="h-full w-full object-cover transition-transform duration-300 group-hover:scale-105" />
        ) : (
          <div className="flex h-full items-center justify-center font-display text-3xl text-pine/40">{r.name.slice(0, 1)}</div>
        )}
      </div>
      <div className="flex flex-1 flex-col gap-2 p-4">
        <div className="flex items-start justify-between gap-2">
          <h3 className="text-lg font-semibold leading-snug">{r.name}</h3>
          <span className="badge shrink-0 bg-turmeric-soft text-amber-800">{r.cuisine}</span>
        </div>
        {r.description && <p className="line-clamp-2 text-sm text-ink-soft">{r.description}</p>}
        <p className="mt-auto pt-2 text-xs text-ink-soft">
          {r.city} &middot; Open {clock(r.openTime)} to {clock(r.closeTime)}
        </p>
      </div>
    </Link>
  );
}
