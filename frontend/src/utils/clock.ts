/**
 * Reads a time typed by a coach: "1:05:30", "30:00" or "30" (minutes, how people say "phút 30").
 * Returns seconds, null for an empty field and NaN when the text is not a time.
 */
export const parseClock = (text: string): number | null => {
  // A Vietnamese IME can leave a tone mark on a digit ("9̣:10") and some keyboards type
  // full-width digits or colons; NFKC folds those, then marks and invisible characters go.
  const value = text
    .normalize('NFKC')
    .replace(/[\p{M}\u200B-\u200D\u2060\uFEFF]/gu, '')
    .replace(/\s+/g, '');
  if (!value) return null;
  if (/^\d+([.,]\d+)?$/.test(value)) return Math.round(parseFloat(value.replace(',', '.')) * 60);
  const parts = value.split(':');
  if (parts.length > 3 || parts.some(p => !/^\d+$/.test(p))) return NaN;
  const numbers = parts.map(Number);
  // Minutes and seconds after the first part must stay under 60.
  if (numbers.slice(1).some(n => n >= 60)) return NaN;
  return numbers.reduce((total, n) => total * 60 + n, 0);
};

/**
 * Formats a time field as it is typed, so nobody has to type the colons: the digits fill
 * from the right, "910" -> "9:10", "1410" -> "14:10", "10530" -> "1:05:30".
 * One or two digits stay as they are, since "30" alone means minute 30 (see finishClockInput).
 */
export const formatClockInput = (text: string): string => {
  const digits = text.normalize('NFKC').replace(/\D/g, '').replace(/^0+(?=\d{3})/, '').slice(0, 6);
  if (digits.length <= 2) return digits;
  const seconds = digits.slice(-2);
  const rest = digits.slice(0, -2);
  if (rest.length <= 2) return `${rest}:${seconds}`;
  return `${rest.slice(0, -2)}:${rest.slice(-2)}:${seconds}`;
};

/** On leaving the field, a bare "30" becomes "30:00" so the minutes reading is visible. */
export const finishClockInput = (text: string): string => (/^\d{1,2}$/.test(text) ? `${text}:00` : text);

/** 95 -> "1:35", 3725 -> "1:02:05". */
export const formatLongClock = (seconds: number): string => {
  const total = Math.max(0, Math.floor(seconds));
  const hours = Math.floor(total / 3600);
  const minutes = Math.floor((total % 3600) / 60);
  const secs = String(total % 60).padStart(2, '0');
  return hours > 0 ? `${hours}:${String(minutes).padStart(2, '0')}:${secs}` : `${minutes}:${secs}`;
};

/** "30:00 – 40:00", "30:00 – hết video", or null for the whole video. */
export const segmentLabel = (start?: number | null, end?: number | null): string | null => {
  if (!start && end == null) return null;
  return `${formatLongClock(start ?? 0)} – ${end != null ? formatLongClock(end) : 'hết video'}`;
};
