// Exit codes: 0 ok | 1 finished but some reservations failed | 2 config | 3 login | 4 page layout changed | 5 backend/other
import { loadConfig, ConfigError } from './config.js';
import { createLogger } from './log.js';
import { parseReservations, LayoutError } from './parse.js';
import { importAll, loadSeen, saveSeen } from './importer.js';
import { reservationsUrl } from './url.js';
import { mapRoomTypes } from './roomTypeMap.js';

const args = new Set(process.argv.slice(2));
const loginOnly = args.has('--login');
const dump = args.has('--dump');
const dryRun = args.has('--dry-run');
// --lookback=30: widen the "modified" window for this run (e.g. to prove parsing on a quiet day).
const lookbackArg = process.argv.slice(2).find((a) => a.startsWith('--lookback='));
let fileLog = null;

async function main() {
  let cfg;
  try {
    cfg = loadConfig({ needSiteMinder: true, lookbackDays: lookbackArg ? Number(lookbackArg.split('=')[1]) : undefined });
  } catch (e) {
    if (e instanceof ConfigError) { console.error(`CONFIG ERROR: ${e.message}`); return 2; }
    throw e;
  }
  const log = fileLog = createLogger(cfg.logFile);
  log.info(`Run started${loginOnly ? ' (login only)' : dryRun ? ' (dry run)' : dump ? ' (dump)' : ''}`);

  const w = new URL(reservationsUrl(cfg)).searchParams;
  log.info(`Window (${w.get('dateType')}): ${w.get('fromDate')} .. ${w.get('toDate')}, pageSize ${cfg.pageSize}`);

  // Playwright is imported lazily so config errors surface even if browsers aren't installed yet.
  const { openBrowser, ensureOnReservations, scrapeTable, dumpPage, saveSession, LoginError, ScrapeError } = await import('./browser.js');
  const headed = loginOnly || args.has('--headed');
  const { browser, context, page } = await openBrowser(cfg, { headed });
  try {
    try {
      await ensureOnReservations(cfg, page, context, { headed, log });
      if (loginOnly) { log.info('Session saved. You can run headless now.'); return 0; }
      if (dump) { await dumpPage(cfg, page, log); return 0; }
      var table = await scrapeTable(cfg, page, log);
      await saveSession(cfg, context);
    } catch (e) {
      if (e instanceof LoginError) { log.error(`LOGIN FAILED: ${e.message}`); return 3; }
      if (e instanceof ScrapeError) {
        log.error(`SCRAPE FAILED: ${e.message}`);
        try { await dumpPage(cfg, page, log); } catch (d) { log.warn(`Could not dump the page: ${d.message}`); }
        return 4;
      }
      throw e;
    }
  } finally {
    await browser.close();
  }

  let parsed;
  try {
    parsed = table.headers ? parseReservations(table, {
      tzOffset: cfg.tzOffset,
      dateOrder: cfg.dateOrder,
      onAssumedDateOrder: (o) => log.warn(`All numeric dates are ambiguous (every day <= 12); assuming ${o === 'mdy' ? 'MM.DD.YYYY' : 'DD.MM.YYYY'}. Verify one date against the SiteMinder screen.`),
    }) : { reservations: [], errors: [] };
  } catch (e) {
    if (e instanceof LayoutError) { log.error(`LAYOUT CHANGED: ${e.message}`); return 4; }
    throw e;
  }
  for (const err of parsed.errors) log.error(`Row ${err.row} could not be parsed: ${err.message}`);
  // Room names are translated here, with the agent's own list; an unknown name is never sent to the backend.
  const roomMapped = mapRoomTypes(parsed.reservations);
  parsed.reservations = roomMapped.reservations;
  const unmapped = roomMapped.unmapped;
  for (const u of unmapped) log.error(`${u.reference}: unmapped room type: "${u.roomTypeName}", skipped - add it to src/roomTypeMap.js`);
  if (parsed.reservations.length === 0 && parsed.errors.length === 0 && unmapped.length === 0) {
    // SiteMinder said "no reservations" (or the table was empty) for this window: normal on a quiet day.
    log.info('No reservations modified in this window');
  }
  if (dryRun) {
    log.info(`Dry run: ${parsed.reservations.length} reservations parsed, ${parsed.errors.length} unparseable rows, ${unmapped.length} skipped for unmapped room type. Nothing sent.`);
    for (const r of parsed.reservations) log.info(`  ${r.reference} ${r.status} ${r.checkIn}..${r.checkOut} ${r.roomTypeName} ${r.totalPrice}`);
    return parsed.errors.length || unmapped.length ? 1 : 0;
  }

  const seen = loadSeen(cfg.stateDir);
  let stats;
  try {
    stats = await importAll(parsed.reservations, { importUrl: cfg.importUrl, integrationKey: cfg.integrationKey, seen, log });
  } finally {
    saveSeen(cfg.stateDir, seen);
  }
  const failed = stats.failed + parsed.errors.length + unmapped.length;
  log.info(
    `SUMMARY found=${stats.found} created=${stats.created} updated=${stats.updated} cancelled=${stats.cancelled} ` +
    `unchanged=${stats.unchanged} skipped_by_backend=${stats.skippedBackend} duplicates_skipped_locally=${stats.skippedLocal} ` +
    `failed=${stats.failed} unparseable_rows=${parsed.errors.length} unmapped_room_type=${unmapped.length}`);
  return failed ? 1 : 0;
}

main().then(
  (code) => { process.exitCode = code; },
  (e) => { (fileLog ?? createLogger(null)).error(`FATAL: ${e.stack ?? e}`); process.exitCode = 5; },
);
