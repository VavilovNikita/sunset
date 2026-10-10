import { mapRoomTypes, ROOM_TYPE_MAP } from '../src/roomTypeMap.js';
import { parseReservations } from '../src/parse.js';
import { test } from 'node:test';
import assert from 'node:assert/strict';
import { addRoomMapping } from '../scripts/add-room-mapping.mjs';

const rooms = [{ id: 'r1', name: 'Sunset Jacuzzi Terrace Room' }, { id: 'r2', name: 'Sunset Terrace Room ABF' }];
function fakeApi(mappings = []) {
  const calls = [];
  const fetchImpl = async (url, opts) => {
    const path = new URL(url).pathname.replace('/api', '');
    calls.push(`${opts.method} ${path}`);
    const ok = (b) => ({ ok: true, status: 200, text: async () => JSON.stringify(b) });
    if (path === '/auth/login') return ok({ token: 't' });
    assert.equal(opts.headers.Authorization, 'Bearer t');
    if (path === '/rooms') return ok(rooms);
    if (opts.method === 'GET') return ok(mappings);
    const body = JSON.parse(opts.body);
    return ok({ id: 'm1', ...body });
  };
  return { fetchImpl, calls };
}
const args = { apiBase: 'http://x/api', email: 'e', password: 'p', siteMinderName: 'Sunset Room with Terrace', roomName: 'Sunset Terrace Room ABF' };

test('maps to the room found by name, not the similarly named jacuzzi type', async () => {
  const api = fakeApi();
  const r = await addRoomMapping({ ...args, fetchImpl: api.fetchImpl });
  assert.equal(r.action, 'created');
  assert.equal(r.mapping.roomId, 'r2');
});

test('running twice changes nothing; a different existing target is refused', async () => {
  const same = await addRoomMapping({ ...args, fetchImpl: fakeApi([{ id: 'm1', siteMinderRoomType: 'sunset room  WITH terrace', roomId: 'r2' }]).fetchImpl });
  assert.equal(same.action, 'unchanged');
  await assert.rejects(addRoomMapping({ ...args, fetchImpl: fakeApi([{ id: 'm1', siteMinderRoomType: 'Sunset Room with Terrace', roomId: 'r1' }]).fetchImpl }), /already mapped to a different/);
});

test('an unknown or ambiguous room name lists what exists', async () => {
  await assert.rejects(addRoomMapping({ ...args, roomName: 'Sunset Terrace Room', fetchImpl: fakeApi().fetchImpl }), /found 0.*Sunset Terrace Room ABF/);
});

const HEADERS = ['Booking reference', 'Guest names', 'Check-in', 'Check-out', 'Channel', 'Room', 'Booked-on date', 'Modified-on date', 'Cancelled-on date', 'Booking status', '-  -', 'Total price'];
const row = (ref, room) => [ref, 'Jane Doe', '08.28.2026', '08.30.2026', 'Direct', room, '07.15.2026', '-', '-', 'Booked', '2 - 0 - 0', 'THB 6266.88'];

test('the room name SiteMinder shows is translated to the sunset room type before sending', () => {
  const parsed = parseReservations({ headers: HEADERS, rows: [row('A1', '1 x Sunset Room with Terrace'), row('A2', '1 x  sunset room WITH terrace ')] });
  const { reservations, unmapped } = mapRoomTypes(parsed.reservations);
  assert.deepEqual(unmapped, []);
  assert.deepEqual(reservations.map((r) => r.roomTypeName), ['Sunset Terrace Room ABF', 'Sunset Terrace Room ABF']);
  assert.equal(ROOM_TYPE_MAP['Sunset Room with Terrace'], 'Sunset Terrace Room ABF');
});

test('an unknown room name is skipped and reported, never passed on; the known ones still go', () => {
  const parsed = parseReservations({ headers: HEADERS, rows: [row('A1', '1 x Sunset Room with Terrace'), row('A2', '1 x Mystery Villa')] });
  const { reservations, unmapped } = mapRoomTypes(parsed.reservations);
  assert.deepEqual(reservations.map((r) => r.reference), ['A1']);
  assert.deepEqual(unmapped, [{ reference: 'A2', roomTypeName: 'Mystery Villa' }]);
});
