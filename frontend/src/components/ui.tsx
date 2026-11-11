import { useEffect, type ReactNode } from 'react';
import { dismiss } from '../uiSlice';
import { useAppDispatch, useAppSelector } from '../store';
import type { BookingStatus, OrderStatus } from '../types';

export function Spinner({ label = 'Loading' }: { label?: string }) {
  return (
    <div role="status" className="flex items-center justify-center gap-3 py-16 text-sm text-ink-soft">
      <span className="h-5 w-5 animate-spin rounded-full border-2 border-line border-t-pine" />
      {label}
    </div>
  );
}

export function ErrorNote({ children }: { children: ReactNode }) {
  return (
    <div role="alert" className="rounded-lg border border-chili/30 bg-chili-soft px-4 py-3 text-sm text-chili">
      {children}
    </div>
  );
}

export function Empty({ title, children }: { title: string; children?: ReactNode }) {
  return (
    <div className="card px-6 py-12 text-center">
      <p className="font-display text-lg font-semibold">{title}</p>
      {children && <div className="mt-2 text-sm text-ink-soft">{children}</div>}
    </div>
  );
}

const BOOKING_STYLE: Record<BookingStatus, string> = {
  CONFIRMED: 'bg-pine-soft text-pine',
  COMPLETED: 'bg-stone-100 text-ink-soft',
  CANCELLED: 'bg-chili-soft text-chili',
  NO_SHOW: 'bg-turmeric-soft text-amber-800',
};
const ORDER_STYLE: Record<OrderStatus, string> = {
  PLACED: 'bg-turmeric-soft text-amber-800',
  PREPARING: 'bg-blue-50 text-blue-800',
  READY: 'bg-pine-soft text-pine',
  SERVED: 'bg-stone-100 text-ink-soft',
  CANCELLED: 'bg-chili-soft text-chili',
};

export function StatusBadge({ status }: { status: BookingStatus | OrderStatus }) {
  const style = (BOOKING_STYLE as Record<string, string>)[status] ?? (ORDER_STYLE as Record<string, string>)[status];
  return <span className={`badge ${style}`}>{status.replace('_', ' ').toLowerCase()}</span>;
}

export function Toaster() {
  const toast = useAppSelector((s) => s.ui.toast);
  const dispatch = useAppDispatch();
  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => dispatch(dismiss()), 4500);
    return () => clearTimeout(t);
  }, [toast, dispatch]);
  if (!toast) return null;
  const tone = toast.kind === 'success' ? 'bg-pine text-white' : 'bg-chili text-white';
  return (
    <div className="pointer-events-none fixed inset-x-0 bottom-6 z-50 flex justify-center px-4">
      <div role="status" className={`pointer-events-auto max-w-md rounded-lg px-4 py-3 text-sm shadow-lg ${tone}`}>
        {toast.text}
      </div>
    </div>
  );
}

export function Pagination({ page, totalPages, onChange }: { page: number; totalPages: number; onChange: (p: number) => void }) {
  if (totalPages <= 1) return null;
  return (
    <div className="mt-6 flex items-center justify-center gap-3 text-sm">
      <button className="btn-ghost btn-sm" disabled={page === 0} onClick={() => onChange(page - 1)}>Previous</button>
      <span className="text-ink-soft">Page {page + 1} of {totalPages}</span>
      <button className="btn-ghost btn-sm" disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>Next</button>
    </div>
  );
}
