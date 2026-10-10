// Pure functions: raw table (headers + cell text) -> reservations in the backend's
// SiteMinderReservationInput shape. No browser, no network, so it is unit-testable.
// Columns are found by header text, never by position, and a missing required column
// throws LayoutError - the page changed and a person has to look, nothing is guessed.

export class LayoutError extends Error {}
export class RowError extends Error {}

const norm = (s) => s.toLowerCase().replace(/[^a-z0-9]+/g, ' ').trim();

// field -> accepted header texts (normalized). First match wins.
const COLUMNS = {
  reference: ['reservation id', 'reservation number', 'reservation no', 'booking id', 'booking reference', 'booking number', 'reference', 'confirmation number'],
  status: ['status', 'reservation status', 'booking status'],
  guest: ['guest names', 'guest name', 'guest', 'name', 'lead guest', 'main guest'],
  checkIn: ['check in', 'checkin', 'arrival', 'arrival date', 'check in date'],
  checkOut: ['check out', 'checkout', 'departure', 'departure date', 'check out date'],
  roomType: ['room type', 'room', 'room name', 'room type name'],
  adults: ['adults', 'adult'],
  children: ['children', 'child', 'kids'],
  infants: ['infants', 'infant'],
  guests: ['guests', 'pax', 'occupancy', 'guests adults children'],
  total: ['total', 'total price', 'total amount', 'total rate', 'grand total', 'price'],
  channel: ['channel', 'source', 'booking channel', 'distribution channel'],
  bookedAt: ['booked on date', 'booked on', 'booked', 'booking date', 'created', 'created on', 'date booked'],
  modifiedAt: ['modified on date', 'modified on', 'modified', 'last modified', 'updated on'],
  cancelledAt: ['cancelled on date', 'cancelled on', 'canceled on', 'canceled on date', 'cancelled', 'cancellation date'],
};
const REQUIRED = ['reference', 'status', 'guest', 'checkIn', 'checkOut', 'roomType', 'total', 'channel', 'bookedAt'];

export function mapColumns(headers) {
  const normalized = headers.map(norm);
  const map = {};
  for (const [field, names] of Object.entries(COLUMNS)) {
    for (const name of names) {
      const i = normalized.indexOf(name);
      if (i >= 0) { map[field] = i; break; }
    }
  }
  // SiteMinder's Occupancy header has no text, only people icons, so its text is just "-  -" (normalizes
  // to ""). With no named occupancy column, take the one textless header sitting between "Booking status"
  // and "Total price"; if that isn't exactly one column, say so rather than pick.
  if (map.adults === undefined && map.guests === undefined && map.status !== undefined && map.total !== undefined) {
    const textless = normalized
      .map((h, i) => (h === '' && i > map.status && i < map.total ? i : -1))
      .filter((i) => i >= 0);
    if (textless.length === 1) map.guests = textless[0];
  }
  const missing = REQUIRED.filter((f) => map[f] === undefined);
  if (map.adults === undefined && map.guests === undefined) missing.push('adults');
  if (missing.length) {
    throw new LayoutError(`Reservations table is missing column(s): ${missing.join(', ')}. Headers seen: ${headers.map((h) => `"${h}"`).join(', ')}`);
  }
  return map;
}

const MONTHS = { jan: 1, feb: 2, mar: 3, apr: 4, may: 5, jun: 6, jul: 7, aug: 8, sep: 9, oct: 10, nov: 11, dec: 12 };
const NUMERIC_DATE = /(\d{1,2})[./](\d{1,2})[./](\d{4})/;
/** Empty, or a placeholder dash (-, –, —) / n/a. */
const isBlank = (v) => !v || /^[-–—\s]*$|^n\/?a$/i.test(v);
const pad = (n) => String(n).padStart(2, '0');

function ymd(y, m, d) {
  const dt = new Date(Date.UTC(y, m - 1, d));
  if (dt.getUTCFullYear() !== y || dt.getUTCMonth() !== m - 1 || dt.getUTCDate() !== d) throw new RowError(`invalid date ${y}-${m}-${d}`);
  return `${y}-${pad(m)}-${pad(d)}`;
}

/**
 * Date text -> YYYY-MM-DD. Accepts 2026-10-12, 12 Oct 2026, Oct 12, 2026, Mon 12 Oct 2026, and the
 * all-numeric forms 08.30.2026 / 08/30/2026, whose field order is `order` ('mdy' | 'dmy'): the text
 * alone can't say (02.10.2026), so parseReservations settles it from the whole table first.
 */
export function parseDate(text, order = 'mdy') {
  const s = text.trim();
  let m;
  if ((m = s.match(/(\d{4})-(\d{2})-(\d{2})/))) return ymd(+m[1], +m[2], +m[3]);
  if ((m = s.match(/(\d{1,2})[\s-]+([A-Za-z]{3})[a-z]*\.?,?[\s-]+(\d{4})/))) {
    const mo = MONTHS[m[2].toLowerCase()];
    if (mo) return ymd(+m[3], mo, +m[1]);
  }
  if ((m = s.match(/([A-Za-z]{3})[a-z]*\.?\s+(\d{1,2}),?\s+(\d{4})/))) {
    const mo = MONTHS[m[1].toLowerCase()];
    if (mo) return ymd(+m[3], mo, +m[2]);
  }
  if ((m = s.match(NUMERIC_DATE))) return order === 'dmy' ? ymd(+m[3], +m[2], +m[1]) : ymd(+m[3], +m[1], +m[2]);
  throw new RowError(`unrecognised date "${text}"`);
}

/** Date+time text -> ISO date-time with offset. Time defaults to 00:00 when absent. */
export function parseDateTime(text, tzOffset, order = 'mdy') {
  const date = parseDate(text, order);
  const t = text.match(/(\d{1,2}):(\d{2})(?::(\d{2}))?\s*(am|pm)?/i);
  let h = 0, min = 0, sec = 0;
  if (t) {
    h = +t[1]; min = +t[2]; sec = t[3] ? +t[3] : 0;
    if (t[4]) {
      const pm = t[4].toLowerCase() === 'pm';
      if (pm && h < 12) h += 12;
      if (!pm && h === 12) h = 0;
    }
  }
  const off = /Z$/.test(text.trim()) ? 'Z' : tzOffset;
  return `${date}T${pad(h)}:${pad(min)}:${pad(sec)}${off}`;
}

export function parseStatus(text) {
  const n = norm(text);
  if (/cancel/.test(n)) return 'CANCELLED';
  if (/modif|amend|chang/.test(n)) return 'MODIFIED';
  if (/book|confirm|new|reserv/.test(n)) return 'BOOKED';
  throw new RowError(`unrecognised status "${text}"`);
}

/** "Doe, Jane" / "Jane Doe" / "Doe" -> { firstName, lastName } */
export function parseName(text) {
  const s = text.replace(/\s+/g, ' ').trim();
  if (!s) throw new RowError('empty guest name');
  if (s.includes(',')) {
    const [last, first] = s.split(',').map((x) => x.trim());
    return { firstName: first || undefined, lastName: last || first };
  }
  const i = s.lastIndexOf(' ');
  return i < 0 ? { lastName: s } : { firstName: s.slice(0, i), lastName: s.slice(i + 1) };
}

/** "฿12,500.00" / "THB 12,500" / "12500" -> { totalPrice: "12500.00", currency? } */
export function parseMoney(text) {
  // Currency and amount can be separate elements, so their text may be glued together ("THB7416.00").
  const cur = text.match(/(?<![A-Za-z])([A-Z]{3})(?![A-Za-z])/);
  const num = text.replace(/,/g, '').match(/\d+(?:\.\d+)?/);
  if (!num) throw new RowError(`unrecognised amount "${text}"`);
  return { totalPrice: Number(num[0]).toFixed(2), currency: cur ? cur[1] : undefined };
}

const count = (text, what) => {
  const m = text.match(/\d+/);
  if (!m) throw new RowError(`unrecognised ${what} count "${text}"`);
  return +m[0];
};

function guestCounts(cell, map) {
  if (map.adults !== undefined) {
    return {
      adults: count(cell(map.adults), 'adult'),
      children: map.children !== undefined && cell(map.children) ? count(cell(map.children), 'child') : 0,
      infants: map.infants !== undefined && cell(map.infants) ? count(cell(map.infants), 'infant') : 0,
    };
  }
  // SiteMinder's Occupancy cell: "2 - 0 - 0" = adults - children - infants (order assumed, not confirmed)
  const dashed = cell(map.guests).match(/^(\d+)\s*-\s*(\d+)\s*-\s*(\d+)$/);
  if (dashed) {
    if (+dashed[1] < 1) throw new RowError(`occupancy "${cell(map.guests)}" has no adults`);
    return { adults: +dashed[1], children: +dashed[2], infants: +dashed[3] };
  }
  // single "Guests" cell, e.g. "2 Adults, 1 Child, 1 Infant"
  const g = cell(map.guests);
  const pick = (re) => { const m = g.match(re); return m ? +m[1] : 0; };
  const adults = pick(/(\d+)\s*adult/i);
  if (!adults) throw new RowError(`no adult count in guests cell "${g}"`);
  return { adults, children: pick(/(\d+)\s*child/i), infants: pick(/(\d+)\s*infant/i) };
}

/**
 * Field order of the all-numeric dates in this table. A first number above 12 can only be a day
 * (dmy), a second number above 12 only a day (mdy); one table showing both is a layout error. When every
 * date is ambiguous (all days <= 12) there is no evidence, so `fallback` decides - and the caller is told.
 */
export function detectDateOrder(table, map, fallback = 'mdy') {
  const cols = ['checkIn', 'checkOut', 'bookedAt', 'modifiedAt', 'cancelledAt'].map((f) => map[f]).filter((i) => i !== undefined);
  let dmy = 0, mdy = 0;
  for (const row of table.rows) {
    for (const i of cols) {
      const m = (row[i] ?? '').match(NUMERIC_DATE);
      if (!m) continue;
      if (+m[1] > 12) dmy++;
      if (+m[2] > 12) mdy++;
    }
  }
  if (dmy && mdy) throw new LayoutError(`Numeric dates in this table contradict each other (${dmy} read only as day-first, ${mdy} only as month-first) - the date format changed`);
  if (dmy) return { order: 'dmy', evidence: true };
  if (mdy) return { order: 'mdy', evidence: true };
  return { order: fallback, evidence: false };
}

/**
 * @param {{headers: string[], rows: string[][]}} table
 * @returns {{reservations: object[], errors: {row: number, message: string}[]}}
 *   A bad row is reported and skipped; a bad layout throws LayoutError.
 */
export function parseReservations(table, { tzOffset = '+07:00', dateOrder = 'mdy', onAssumedDateOrder } = {}) {
  const map = mapColumns(table.headers);
  const detected = detectDateOrder(table, map, dateOrder);
  const order = detected.order;
  if (!detected.evidence && table.rows.some((r) => r.some((c) => NUMERIC_DATE.test(c)))) onAssumedDateOrder?.(order);
  const reservations = [];
  const errors = [];
  table.rows.forEach((cells, idx) => {
    if (cells.every((c) => !c.trim())) return;
    const cell = (i) => (i === undefined ? '' : (cells[i] ?? '').replace(/\s+/g, ' ').trim());
    try {
      const reference = cell(map.reference);
      if (!reference) throw new RowError('empty reservation reference');
      const status = parseStatus(cell(map.status));
      const { totalPrice, currency } = parseMoney(cell(map.total));
      const r = {
        reference,
        status,
        ...parseName(cell(map.guest)),
        checkIn: parseDate(cell(map.checkIn), order),
        checkOut: parseDate(cell(map.checkOut), order),
        roomTypeName: cell(map.roomType),
        ...guestCounts(cell, map),
        totalPrice,
        channel: cell(map.channel),
        bookedAt: parseDateTime(cell(map.bookedAt), tzOffset, order),
      };
      if (!r.roomTypeName) throw new RowError('empty room type');
      if (!r.channel) throw new RowError('empty channel');
      if (currency) r.currency = currency;
      // An empty optional date is shown as "-" on the page.
      const optionalDateTime = (i) => (isBlank(cell(i)) ? undefined : parseDateTime(cell(i), tzOffset, order));
      const modifiedAt = optionalDateTime(map.modifiedAt);
      const cancelledAt = optionalDateTime(map.cancelledAt);
      if (modifiedAt) r.modifiedAt = modifiedAt;
      if (cancelledAt) r.cancelledAt = cancelledAt;
      // The backend orders versions by the latest of these timestamps; a cancelled row
      // without any cancel time would otherwise look stale against its own booking time.
      if (status === 'CANCELLED' && !r.cancelledAt) r.cancelledAt = r.modifiedAt ?? r.bookedAt;
      reservations.push(r);
    } catch (e) {
      if (!(e instanceof RowError)) throw e;
      errors.push({ row: idx + 1, message: e.message });
    }
  });
  return { reservations, errors };
}
