import { describe, expect, it } from 'vitest';
import { addDays, clock, errorMessage, groupBy, isoDate } from './format';

describe('format helpers', () => {
  it('formats clock times', () => {
    expect(clock('11:00:00')).toBe('11:00 AM');
    expect(clock('00:30:00')).toBe('12:30 AM');
    expect(clock('23:00:00')).toBe('11:00 PM');
    expect(clock('12:00:00')).toBe('12:00 PM');
  });

  it('builds local ISO dates', () => {
    expect(isoDate(new Date(2030, 0, 5))).toBe('2030-01-05');
    expect(isoDate(addDays(new Date(2030, 0, 31), 1))).toBe('2030-02-01');
  });

  it('groups by key and keeps order', () => {
    const grouped = groupBy([{ c: 'a', n: 1 }, { c: 'b', n: 2 }, { c: 'a', n: 3 }], (x) => x.c);
    expect(grouped.a.map((x) => x.n)).toEqual([1, 3]);
    expect(Object.keys(grouped)).toEqual(['a', 'b']);
  });

  it('extracts server error messages', () => {
    expect(errorMessage({ data: { detail: 'No table is free' } })).toBe('No table is free');
    expect(errorMessage({ data: { errors: { email: 'must be a well-formed email address' } } })).toContain('email:');
    expect(errorMessage({ status: 'FETCH_ERROR' })).toContain('Cannot reach');
    expect(errorMessage(undefined)).toContain('Something went wrong');
  });
});
