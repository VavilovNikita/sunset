import { test } from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { reservationsUrl, dateInZone } from '../src/url.js';
import { parseReservations } from '../src/parse.js';

const cfg = {
  reservationsBase: 'https://platform.siteminder.com/reservations',
  propertyId: '455c0edf-8114-11e5-8827-02b1347ffa5b',
  hotelTimeZone: 'Asia/Bangkok', lookbackDays: 3, pageSize: 10, extraQuery: '',
};

test('window is computed in hotel time, not UTC', () => {
  // 20:00 UTC on 10 Oct is already 11 Oct 03:00 in Bangkok
  const now = new Date('2026-10-10T20:00:00Z');
  assert.equal(dateInZone(now, 'Asia/Bangkok'), '2026-10-11');
  assert.equal(dateInZone(now, 'UTC'), '2026-10-10');
  const u = new URL(reservationsUrl(cfg, 2, now));
  assert.equal(u.pathname, '/reservations/455c0edf-8114-11e5-8827-02b1347ffa5b/search');
  assert.equal(u.searchParams.get('dateType'), 'ModifiedAt');
  assert.equal(u.searchParams.get('fromDate'), '2026-10-08');
  assert.equal(u.searchParams.get('toDate'), '2026-10-12');
  assert.equal(u.searchParams.get('page'), '2');
  assert.equal(u.searchParams.get('pageSize'), '10');
  assert.equal(u.searchParams.get('sortBy'), 'checkInDate');
  assert.equal(u.searchParams.get('sortOrder'), 'asc');
});

test('window crosses month and year boundaries and takes extra query params', () => {
  const u = new URL(reservationsUrl({ ...cfg, lookbackDays: 5, extraQuery: 'sortBy=x&sortOrder=desc' }, 1, new Date('2026-12-31T05:00:00Z')));
  assert.equal(u.searchParams.get('fromDate'), '2026-12-26');
  assert.equal(u.searchParams.get('toDate'), '2027-01-01');
  assert.equal(u.searchParams.get('sortBy'), 'x');
});

const REAL_HEADERS = ['Booking reference', 'Guest names', 'Check-in', 'Check-out', 'Channel', 'Room', 'Booked-on date', 'Modified-on date', 'Cancelled-on date', 'Booking status', 'Occupancy', 'Total price'];

test('the real SiteMinder formats parse: MM.DD.YYYY, "2 - 0 - 0", Booked, glued currency + amount', () => {
  const row = ['SM-77', 'Jane Doe', '08.28.2026', '08.30.2026', 'Booking.com', 'Garden Villa', '07.15.2026, 08:47 PM', '08.20.2026, 10:00 AM', '', 'Booked', '2 - 1 - 0', 'THB7416.00'];
  const { reservations, errors } = parseReservations({ headers: REAL_HEADERS, rows: [row] });
  assert.deepEqual(errors, []);
  const r = reservations[0];
  assert.deepEqual([r.checkIn, r.checkOut, r.status], ['2026-08-28', '2026-08-30', 'BOOKED']);
  assert.equal(r.bookedAt, '2026-07-15T20:47:00+07:00');
  assert.equal(r.modifiedAt, '2026-08-20T10:00:00+07:00');
  assert.deepEqual([r.adults, r.children, r.infants], [2, 1, 0]);
  assert.equal(r.totalPrice, '7416.00');
  assert.equal(r.currency, 'THB');
  assert.equal(r.roomTypeName, 'Garden Villa');
});

// Regression: an unmodified / uncancelled booking shows "-" in those date cells ("unrecognised date \"-\"").
test('placeholder dashes in optional date cells mean no date', () => {
  for (const dash of ['-', '–', '—', ' - ', 'N/A']) {
    const row = ['SM-1', 'Jane Doe', '08.28.2026', '08.30.2026', 'Direct', 'Villa', '07.15.2026, 08:47 PM', dash, dash, 'Booked', '2 - 0 - 0', 'THB 100.00'];
    const { reservations, errors } = parseReservations({ headers: REAL_HEADERS, rows: [row] });
    assert.deepEqual(errors, [], `dash ${JSON.stringify(dash)}`);
    assert.equal(reservations[0].modifiedAt, undefined);
    assert.equal(reservations[0].cancelledAt, undefined);
  }
  // but a dash where a date is required stays an error
  const bad = ['SM-1', 'Jane Doe', '-', '08.30.2026', 'Direct', 'Villa', '07.15.2026', '', '', 'Booked', '2 - 0 - 0', 'THB 100.00'];
  assert.equal(parseReservations({ headers: REAL_HEADERS, rows: [bad] }).errors.length, 1);
});

// Regression: "invalid date 2026-30-8" - a cell like 08.30.2026 was read day-first. A day above 12 is the
// only hard evidence of field order, so the table decides it, and ambiguous dates fall back to a stated default.
test('numeric date order is detected from any date above 12 in the table', () => {
  const mk = (checkIn, checkOut) => ['SM-1', 'A B', checkIn, checkOut, 'Direct', 'Villa', '01.02.2026', '', '', 'Booked', '1 - 0 - 0', 'THB 100.00'];
  const one = (row, opts) => parseReservations({ headers: REAL_HEADERS, rows: [row] }, opts);
  // 08.30.2026 can only be Aug 30 -> month-first, and the ambiguous 08.05.2026 in the same table follows it
  const mdy = one(mk('08.05.2026', '08.30.2026'));
  assert.deepEqual(mdy.errors, []);
  assert.deepEqual([mdy.reservations[0].checkIn, mdy.reservations[0].checkOut], ['2026-08-05', '2026-08-30']);
  // 30.08.2026 can only be 30 Aug -> day-first, even though the default is month-first
  const dmy = one(mk('05.08.2026', '30.08.2026'));
  assert.deepEqual([dmy.reservations[0].checkIn, dmy.reservations[0].checkOut], ['2026-08-05', '2026-08-30']);
  // evidence from a different column (booked-on) counts too
  const viaBooked = one(['SM-1', 'A B', '05.08.2026', '06.08.2026', 'Direct', 'Villa', '30.07.2026', '', '', 'Booked', '1 - 0 - 0', 'THB 1.00']);
  assert.equal(viaBooked.reservations[0].checkIn, '2026-08-05');
  // both readings in one table: the format changed under us, do not guess
  assert.throws(() => parseReservations({ headers: REAL_HEADERS, rows: [mk('30.08.2026', '08.30.2026')] }), /contradict/);
});

test('all-ambiguous dates use the configured default and say so', () => {
  const row = ['SM-1', 'A B', '02.10.2026', '05.10.2026', 'Direct', 'Villa', '01.09.2026', '', '', 'Booked', '1 - 0 - 0', 'THB 100.00'];
  let assumed = null;
  const def = parseReservations({ headers: REAL_HEADERS, rows: [row] }, { onAssumedDateOrder: (o) => { assumed = o; } });
  assert.equal(assumed, 'mdy');
  assert.equal(def.reservations[0].checkIn, '2026-02-10');
  const d = parseReservations({ headers: REAL_HEADERS, rows: [row] }, { dateOrder: 'dmy' });
  assert.equal(d.reservations[0].checkIn, '2026-10-02');
});

test('the textless occupancy header ("-  -", icons only) is found by its position', () => {
  const headers = REAL_HEADERS.map((h) => (h === 'Occupancy' ? '-  -' : h));
  const row = ['SM-7', 'Jane Doe', '02.10.2026', '05.10.2026', 'Direct', 'Villa', '01.09.2026, 08:47 PM', '', '', 'Booked', '2 - 1 - 0', 'THB 7416.00'];
  const { reservations, errors } = parseReservations({ headers, rows: [row] });
  assert.deepEqual(errors, []);
  assert.deepEqual([reservations[0].adults, reservations[0].children, reservations[0].infants], [2, 1, 0]);
  // an unrelated textless column elsewhere (say, an actions column) must not be mistaken for it
  const withActions = [...headers, ''];
  assert.equal(parseReservations({ headers: withActions, rows: [[...row, '']] }).reservations[0].adults, 2);
  // two candidates between status and total: ambiguous, so a layout error instead of a guess
  const ambiguous = [...headers.slice(0, 10), '', ...headers.slice(10)];
  assert.throws(() => parseReservations({ headers: ambiguous, rows: [] }), /missing column\(s\): adults/);
});

test('currency may also be spaced or separated by a line break; a foreign currency is passed on for the backend to reject', () => {
  const mk = (total) => parseReservations({ headers: REAL_HEADERS, rows: [['SM-1', 'A B', '02.10.2026', '03.10.2026', 'Direct', 'Villa', '01.09.2026', '', '', 'Booked', '1 - 0 - 0', total]] }).reservations[0];
  assert.deepEqual([mk('THB 7,416.00').totalPrice, mk('THB 7,416.00').currency], ['7416.00', 'THB']);
  assert.equal(mk('THB\n7416.00').totalPrice, '7416.00');
  assert.equal(mk('USD 99.00').currency, 'USD');
});

test('an occupancy with no adults, or a status we do not know, is a reported row error', () => {
  const base = ['SM-1', 'A B', '02.10.2026', '03.10.2026', 'Direct', 'Villa', '01.09.2026', '', '', 'Booked', '1 - 0 - 0', 'THB 100.00'];
  const bad = (i, v) => { const r = [...base]; r[i] = v; return parseReservations({ headers: REAL_HEADERS, rows: [r] }); };
  assert.equal(bad(10, '0 - 2 - 0').errors.length, 1);
  assert.match(bad(9, 'Pending review').errors[0].message, /unrecognised status "Pending review"/);
});

// End to end through real Chromium against a local stand-in for the Reservations page.
test('scrapeTable pages through the window and treats "no reservations" as empty', async (t) => {
  let chromium;
  try { ({ chromium } = await import('playwright')); const b = await chromium.launch(); await b.close(); } catch { t.skip('Chromium not installed'); return; }
  const { scrapeTable } = await import('../src/browser.js');
  const row = (n) => `<tr><td>SM-${n}</td><td>Guest ${n}</td><td>10.12.2026</td><td>10.13.2026</td><td>Direct</td><td>Villa</td><td>10.01.2026, 08:47 PM</td><td>10.09.2026, 10:00 AM</td><td></td><td>Booked</td><td>2 - 0 - 0</td><td><span>THB</span><span>1000.00</span></td></tr>`;
  const table = (rows) => `<table><thead><tr>${REAL_HEADERS.map((h) => `<th>${h}</th>`).join('')}</tr></thead><tbody>${rows.join('')}</tbody></table>`;
  const server = http.createServer((req, res) => {
    const u = new URL(req.url, 'http://x');
    res.setHeader('content-type', 'text/html');
    if (u.pathname.includes('empty')) return res.end('<body><h2>Looks like there are no reservations</h2></body>');
    const page = Number(u.searchParams.get('page'));
    const pages = { 1: [1, 2], 2: [3] };
    res.end(pages[page] ? `<body>${table(pages[page].map(row))}</body>` : '<body><h2>Looks like there are no reservations</h2></body>');
  }).listen(0);
  const base = `http://127.0.0.1:${server.address().port}`;
  const browser = await chromium.launch();
  const log = { info() {}, warn() {}, error() {} };
  try {
    const page = await browser.newPage();
    const c = { ...cfg, reservationsBase: `${base}/r`, tableSelector: 'table', maxPages: 10, pageSize: 2 };
    await page.goto(reservationsUrl(c, 1));
    const got = await scrapeTable(c, page, log);
    assert.equal(got.rows.length, 3);
    const parsed = parseReservations(got);
    assert.deepEqual(parsed.errors, []);
    assert.equal(parsed.reservations.length, 3);
    assert.equal(parsed.reservations[0].totalPrice, '1000.00');

    const e = { ...c, propertyId: 'empty' };
    await page.goto(reservationsUrl(e, 1));
    const none = await scrapeTable(e, page, log);
    assert.equal(none.headers, null);
    assert.deepEqual(none.rows, []);
  } finally {
    await browser.close();
    server.close();
  }
});
