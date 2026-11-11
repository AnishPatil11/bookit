const inr = new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 });

export const money = (value: number): string => inr.format(value);

/** "11:00:00" -> "11:00 AM" */
export function clock(time: string): string {
  const [h, m] = time.split(':').map(Number);
  const suffix = h >= 12 ? 'PM' : 'AM';
  const hour = h % 12 === 0 ? 12 : h % 12;
  return `${hour}:${String(m).padStart(2, '0')} ${suffix}`;
}

/** Server timestamps have no zone ("2026-10-26T19:00:00"); new Date() reads them as local time. */
export function dateTime(iso: string): string {
  return new Date(iso).toLocaleString('en-IN', {
    weekday: 'short', day: 'numeric', month: 'short', hour: 'numeric', minute: '2-digit',
  });
}

export function timeOnly(iso: string): string {
  return new Date(iso).toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' });
}

/** YYYY-MM-DD for <input type="date"> in the user's local calendar. */
export function isoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, '0');
  const day = String(d.getDate()).padStart(2, '0');
  return `${y}-${m}-${day}`;
}

export function addDays(d: Date, days: number): Date {
  const copy = new Date(d);
  copy.setDate(copy.getDate() + days);
  return copy;
}

export function groupBy<T>(items: T[], key: (item: T) => string): Record<string, T[]> {
  return items.reduce<Record<string, T[]>>((acc, item) => {
    const k = key(item);
    (acc[k] ||= []).push(item);
    return acc;
  }, {});
}

/** Pulls a human-readable message out of whatever RTK Query hands back on failure. */
export function errorMessage(err: unknown): string {
  const e = err as { status?: number | string; data?: { detail?: string; errors?: Record<string, string> } };
  if (e?.data?.errors) {
    return Object.entries(e.data.errors).map(([field, msg]) => `${field}: ${msg}`).join('. ');
  }
  if (e?.data?.detail) return e.data.detail;
  if (e?.status === 'FETCH_ERROR') return 'Cannot reach the server. Is the backend running?';
  return 'Something went wrong. Please try again.';
}
