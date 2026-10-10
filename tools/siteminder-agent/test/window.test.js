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
});

test('window crosses month and year boundaries and takes extra query params', () => {
  const u = new URL(reservationsUrl({ ...cfg, lookbackDays: 5, extraQuery: 'sortBy=x&sortOrder=desc' }, 1, new Date('2026-12-31T05:00:00Z')));
  assert.equal(u.searchParams.get('fromDate'), '2026-12-26');
  assert.equal(u.searchParams.get('toDate'), '2027-01-01');
  assert.equal(u.searchParams.get('sortBy'), 'x');
});

const REAL_HEADERS = ['Booking reference', 'Guest names', 'Check-in', 'Check-out', 'Channel', 'Room', 'Booked-on date', 'Modified-on date', 'Cancelled-on date', 'Booking status', 'Occupancy', 'Total price'];

test('the headers SiteMinder really shows are all recognised', () => {
  const row = ['SM-77', 'Jane Doe', '12 Oct 2026', '15 Oct 2026', 'Booking.com', 'Garden Villa', '01 Oct 2026 14:05', '09 Oct 2026 10:00', '', 'Confirmed', '2 Adults, 1 Child', '฿12,500.00'];
  const { reservations, errors } = parseReservations({ headers: REAL_HEADERS, rows: [row] });
  assert.deepEqual(errors, []);
  assert.equal(reservations[0].modifiedAt, '2026-10-09T10:00:00+07:00');
  assert.equal(reservations[0].children, 1);
  assert.equal(reservations[0].roomTypeName, 'Garden Villa');
});

// End to end through real Chromium against a local stand-in for the Reservations page.
test('scrapeTable pages through the window and treats "no reservations" as empty', async (t) => {
  let chromium;
  try { ({ chromium } = await import('playwright')); const b = await chromium.launch(); await b.close(); } catch { t.skip('Chromium not installed'); return; }
  const { scrapeTable } = await import('../src/browser.js');
  const row = (n) => `<tr><td>SM-${n}</td><td>Guest ${n}</td><td>12 Oct 2026</td><td>13 Oct 2026</td><td>Direct</td><td>Villa</td><td>01 Oct 2026</td><td>09 Oct 2026</td><td></td><td>Confirmed</td><td>2 Adults</td><td>฿1,000.00</td></tr>`;
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
    assert.equal(parseReservations(got).reservations.length, 3);

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
