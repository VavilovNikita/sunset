import { test } from 'node:test';
import assert from 'node:assert/strict';
import { parseReservations, parseDate, parseDateTime, parseMoney, parseName, LayoutError } from '../src/parse.js';
import { importAll, hashOf } from '../src/importer.js';

const headers = ['Reservation ID', 'Status', 'Guest name', 'Check-in', 'Check-out', 'Room type', 'Adults', 'Children', 'Total', 'Channel', 'Booked on', 'Modified on'];
const row = (o = {}) => Object.values({
  id: 'SM-1001', status: 'Confirmed', guest: 'Doe, Jane', ci: '12 Oct 2026', co: '15 Oct 2026', room: 'Garden Jacuzzi Villa ABF',
  ad: '2', ch: '1', total: '฿12,500.00', channel: 'Booking.com', booked: '01 Oct 2026 14:05', modified: '', ...o,
});

test('parses a normal row into the backend contract', () => {
  const { reservations, errors } = parseReservations({ headers, rows: [row()] });
  assert.deepEqual(errors, []);
  assert.deepEqual(reservations[0], {
    reference: 'SM-1001', status: 'BOOKED', firstName: 'Jane', lastName: 'Doe', checkIn: '2026-10-12', checkOut: '2026-10-15',
    roomTypeName: 'Garden Jacuzzi Villa ABF', adults: 2, children: 1, infants: 0, totalPrice: '12500.00',
    channel: 'Booking.com', bookedAt: '2026-10-01T14:05:00+07:00',
  });
});

test('cancelled row without a cancel time still gets a timestamp', () => {
  const { reservations } = parseReservations({ headers, rows: [row({ status: 'Cancelled', modified: '05 Oct 2026 09:00' })] });
  assert.equal(reservations[0].status, 'CANCELLED');
  assert.equal(reservations[0].cancelledAt, '2026-10-05T09:00:00+07:00');
});

test('a bad row is reported, the good ones still parse', () => {
  const { reservations, errors } = parseReservations({ headers, rows: [row({ ci: 'soon' }), row({ id: 'SM-2' }), row({ status: 'Weird' })] });
  assert.equal(reservations.length, 1);
  assert.equal(errors.length, 2);
  assert.equal(errors[0].row, 1);
});

test('missing required column is a layout error, not a guess', () => {
  assert.throws(() => parseReservations({ headers: headers.filter((h) => h !== 'Total'), rows: [] }), LayoutError);
});

test('guests in a single cell', () => {
  const h = headers.filter((h) => h !== 'Adults' && h !== 'Children').concat('Guests');
  const cells = row();
  cells.splice(6, 2); // drop adults+children values
  cells.push('2 Adults, 1 Child, 1 Infant');
  const { reservations } = parseReservations({ headers: h, rows: [cells] });
  assert.deepEqual([reservations[0].adults, reservations[0].children, reservations[0].infants], [2, 1, 1]);
});

test('date, money and name helpers', () => {
  assert.equal(parseDate('Mon, 12 Oct 2026'), '2026-10-12');
  assert.equal(parseDate('Oct 12, 2026'), '2026-10-12');
  assert.equal(parseDate('12/10/2026'), '2026-10-12');
  assert.throws(() => parseDate('31 Feb 2026'));
  assert.equal(parseDateTime('01 Oct 2026 2:05 PM', '+07:00'), '2026-10-01T14:05:00+07:00');
  assert.deepEqual(parseMoney('THB 1,234'), { totalPrice: '1234.00', currency: 'THB' });
  assert.deepEqual(parseMoney('USD 99.5'), { totalPrice: '99.50', currency: 'USD' });
  assert.deepEqual(parseName('Smith'), { lastName: 'Smith' });
  assert.deepEqual(parseName('Mary Ann Smith'), { firstName: 'Mary Ann', lastName: 'Smith' });
});

const quietLog = { info() {}, warn() {}, error() {} };
const reply = (status, body) => async () => ({ status, ok: status < 300, text: async () => JSON.stringify(body) });

test('importer posts, remembers success and skips unchanged next time', async () => {
  const { reservations } = parseReservations({ headers, rows: [row()] });
  const seen = {};
  let calls = 0;
  const f = async (url, opts) => {
    calls++;
    assert.equal(opts.headers['X-Integration-Key'], 'k');
    return reply(200, { action: 'CREATED', bookingId: 'b1', changes: [], warnings: [], message: null })();
  };
  const ctx = { importUrl: 'http://x', integrationKey: 'k', seen, log: quietLog, fetchImpl: f };
  assert.equal((await importAll(reservations, ctx)).created, 1);
  const second = await importAll(reservations, ctx);
  assert.equal(second.skippedLocal, 1);
  assert.equal(calls, 1);
  // a changed price is a different payload and goes again
  const changed = [{ ...reservations[0], totalPrice: '13000.00' }];
  assert.notEqual(hashOf(changed[0]), hashOf(reservations[0]));
  assert.equal((await importAll(changed, ctx)).created, 1);
  assert.equal(calls, 2);
});

test('importer does not remember failures and aborts on 401', async () => {
  const { reservations } = parseReservations({ headers, rows: [row(), row({ id: 'SM-2' })] });
  const seen = {};
  const s = await importAll(reservations, { importUrl: 'x', integrationKey: 'k', seen, log: quietLog, fetchImpl: reply(409, { error: 'Selected dates are no longer available' }) });
  assert.equal(s.failed, 2);
  assert.deepEqual(seen, {});
  await assert.rejects(importAll(reservations, { importUrl: 'x', integrationKey: 'bad', seen, log: quietLog, fetchImpl: reply(401, { error: 'no' }) }), /401/);
});
