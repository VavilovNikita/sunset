// One-off helper: map a SiteMinder room type name to a sunset room type, through the same API the
// admin would use (POST /integrations/siteminder/room-type-mappings, MANAGER+). Looks the room up by
// name, so no database ids have to be known or copied.
//
//   node scripts/add-room-mapping.mjs "Sunset Room with Terrace" "Sunset Terrace Room ABF"
//
// Needs SUNSET_STAFF_EMAIL / SUNSET_STAFF_PASSWORD (a MANAGER or ADMIN login) in .env or the environment,
// and SUNSET_IMPORT_URL to find the backend. Safe to run twice: an identical mapping is left alone.
import { pathToFileURL } from 'node:url';
import { loadDotEnv } from '../src/config.js';

const norm = (s) => s.trim().replace(/\s+/g, ' ').toLowerCase();

export async function addRoomMapping({ apiBase, email, password, siteMinderName, roomName, fetchImpl = fetch }) {
  const call = async (method, path, body, token) => {
    const res = await fetchImpl(`${apiBase}${path}`, {
      method,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: body ? JSON.stringify(body) : undefined,
    });
    const text = await res.text();
    if (!res.ok) throw new Error(`${method} ${path} -> HTTP ${res.status} ${text.slice(0, 300)}`);
    return text ? JSON.parse(text) : null;
  };

  const { token } = await call('POST', '/auth/login', { email, password });
  const rooms = await call('GET', '/rooms', null, token);
  const matches = rooms.filter((r) => norm(r.name) === norm(roomName));
  if (matches.length !== 1) {
    throw new Error(`Expected exactly one room type named "${roomName}", found ${matches.length}. Room types: ${rooms.map((r) => `"${r.name}"`).join(', ')}`);
  }
  const room = matches[0];

  const existing = (await call('GET', '/integrations/siteminder/room-type-mappings', null, token))
    .find((m) => norm(m.siteMinderRoomType) === norm(siteMinderName));
  if (existing) {
    if (existing.roomId === room.id) return { action: 'unchanged', mapping: existing, room };
    throw new Error(`"${siteMinderName}" is already mapped to a different room type (${existing.roomId}). Change it in the admin (PUT /integrations/siteminder/room-type-mappings/${existing.id}) - not overwritten here.`);
  }
  const mapping = await call('POST', '/integrations/siteminder/room-type-mappings', { siteMinderRoomType: siteMinderName, roomId: room.id }, token);
  return { action: 'created', mapping, room };
}

if (import.meta.url === pathToFileURL(process.argv[1]).href) {
  loadDotEnv();
  const [siteMinderName, roomName] = process.argv.slice(2);
  const { SUNSET_STAFF_EMAIL: email, SUNSET_STAFF_PASSWORD: password, SUNSET_IMPORT_URL } = process.env;
  if (!siteMinderName || !roomName || !email || !password || !SUNSET_IMPORT_URL) {
    console.error('Usage: node scripts/add-room-mapping.mjs "<SiteMinder room type>" "<sunset room type>"\nNeeds SUNSET_STAFF_EMAIL, SUNSET_STAFF_PASSWORD, SUNSET_IMPORT_URL.');
    process.exit(2);
  }
  const apiBase = new URL(SUNSET_IMPORT_URL).origin + '/api';
  addRoomMapping({ apiBase, email, password, siteMinderName, roomName }).then(
    (r) => console.log(`${r.action}: "${siteMinderName}" -> ${r.room.name} (${r.room.id})`),
    (e) => { console.error(`FAILED: ${e.message}`); process.exit(1); },
  );
}
