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
