import { createHash } from 'node:crypto';
import { readFileSync, writeFileSync, mkdirSync, renameSync, existsSync } from 'node:fs';
import { join } from 'node:path';

/**
 * Local memory of what was already accepted by the backend, reference -> payload hash.
 * The backend is the real idempotency authority (it diffs against the booking); this only
 * saves re-posting reservations that haven't changed since the last successful run.
 * Losing the file is harmless: everything is re-posted and the backend answers UNCHANGED.
 */
export function loadSeen(stateDir) {
  const file = join(stateDir, 'seen.json');
  if (!existsSync(file)) return {};
  try { return JSON.parse(readFileSync(file, 'utf8')); } catch { return {}; }
}

export function saveSeen(stateDir, seen) {
  mkdirSync(stateDir, { recursive: true });
  const file = join(stateDir, 'seen.json');
  writeFileSync(file + '.tmp', JSON.stringify(seen, null, 1), { mode: 0o600 });
  renameSync(file + '.tmp', file);
}

export const hashOf = (reservation) => createHash('sha256').update(JSON.stringify(reservation)).digest('hex');

/**
 * Posts reservations one by one. A failure on one reservation is recorded and the rest continue.
 * Only a SUCCESSFUL post (including SKIPPED/UNCHANGED answers) is remembered, so 400/409/5xx
 * are retried on the next run.
 */
export async function importAll(reservations, { importUrl, integrationKey, seen, log, fetchImpl = fetch }) {
  const stats = { found: reservations.length, created: 0, updated: 0, cancelled: 0, unchanged: 0, skippedBackend: 0, skippedLocal: 0, failed: 0 };
  for (const r of reservations) {
    const hash = hashOf(r);
    if (seen[r.reference] === hash) { stats.skippedLocal++; continue; }
    let res;
    try {
      res = await fetchImpl(importUrl, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'X-Integration-Key': integrationKey },
        body: JSON.stringify(r),
        signal: AbortSignal.timeout(30_000),
      });
    } catch (e) {
      stats.failed++;
      log.error(`${r.reference}: backend unreachable (${e.message})`);
      continue;
    }
    const text = await res.text();
    if (res.status === 401) {
      // Wrong/missing key affects every reservation - stop instead of logging N identical failures.
      throw new Error('Backend answered 401: SUNSET_INTEGRATION_KEY is wrong or SITEMINDER_INTEGRATION_KEY is not set on the backend');
    }
    if (!res.ok) {
      stats.failed++;
      log.error(`${r.reference}: HTTP ${res.status} ${summarizeError(text)}`);
      continue;
    }
    let body;
    try { body = JSON.parse(text); } catch { body = {}; }
    seen[r.reference] = hash;
    switch (body.action) {
      case 'CREATED': stats.created++; break;
      case 'UPDATED': stats.updated++; break;
      case 'CANCELLED': stats.cancelled++; break;
      case 'UNCHANGED': stats.unchanged++; break;
      default: stats.skippedBackend++;
    }
    const extra = [...(body.changes ?? []), ...(body.warnings ?? []).map((w) => `WARNING: ${w}`), body.message].filter(Boolean);
    log.info(`${r.reference}: ${body.action}${body.bookingId ? ` booking=${body.bookingId}` : ''}${extra.length ? ` | ${extra.join(' | ')}` : ''}`);
    for (const w of body.warnings ?? []) log.warn(`${r.reference}: ${w}`);
  }
  return stats;
}

function summarizeError(text) {
  try {
    const j = JSON.parse(text);
    if (j.fieldErrors) return JSON.stringify(j.fieldErrors);
    return j.error ?? j.message ?? text.slice(0, 300);
  } catch {
    return text.slice(0, 300);
  }
}
