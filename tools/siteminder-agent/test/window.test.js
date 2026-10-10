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

test('the real SiteMinder formats parse: DD.MM.YYYY, "2 - 0 - 0", Booked, glued currency + amount', () => {
  const row = ['SM-77', 'Jane Doe', '02.10.2026', '05.10.2026', 'Booking.com', 'Garden Villa', '01.09.2026, 08:47 PM', '09.09.2026, 10:00 AM', '', 'Booked', '2 - 1 - 0', 'THB7416.00'];
  const { reservations, errors } = parseReservations({ headers: REAL_HEADERS, rows: [row] });
  assert.deepEqual(errors, []);
  const r = reservations[0];
  assert.deepEqual([r.checkIn, r.checkOut, r.status], ['2026-10-02', '2026-10-05', 'BOOKED']);
  assert.equal(r.bookedAt, '2026-09-01T20:47:00+07:00');
  assert.equal(r.modifiedAt, '2026-09-09T10:00:00+07:00');
  assert.deepEqual([r.adults, r.children, r.infants], [2, 1, 0]);
  assert.equal(r.totalPrice, '7416.00');
  assert.equal(r.currency, 'THB');
  assert.equal(r.roomTypeName, 'Garden Villa');
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
  const row = (n) => `<tr><td>SM-${n}</td><td>Guest ${n}</td><td>12.10.2026</td><td>13.10.2026</td><td>Direct</td><td>Villa</td><td>01.10.2026, 08:47 PM</td><td>09.10.2026, 10:00 AM</td><td></td><td>Booked</td><td>2 - 0 - 0</td><td><span>THB</span><span>1000.00</span></td></tr>`;
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
