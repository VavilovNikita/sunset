// The Reservations search URL is built fresh on every run: a static URL with fixed dates goes stale the
// next day (and an empty range makes SiteMinder render no table at all). Pure, so it can be unit-tested.

const DAY_MS = 24 * 60 * 60 * 1000;

/** Calendar date (YYYY-MM-DD) of `now` in the given IANA time zone, shifted by `plusDays`. */
export function dateInZone(now, timeZone, plusDays = 0) {
  // en-CA formats as YYYY-MM-DD.
  const today = new Intl.DateTimeFormat('en-CA', { timeZone, year: 'numeric', month: '2-digit', day: '2-digit' }).format(now);
  if (!plusDays) return today;
  const [y, m, d] = today.split('-').map(Number);
  return new Date(Date.UTC(y, m - 1, d) + plusDays * DAY_MS).toISOString().slice(0, 10);
}

/**
 * Window by "modified" date: from `lookbackDays` ago (catches edits made while a previous run was down)
 * to tomorrow (a change late in the hotel's day must not fall outside `toDate`).
 */
export function reservationsUrl(cfg, page = 1, now = new Date()) {
  const q = new URLSearchParams({
    dateType: 'ModifiedAt',
    fromDate: dateInZone(now, cfg.hotelTimeZone, -cfg.lookbackDays),
    toDate: dateInZone(now, cfg.hotelTimeZone, 1),
    page: String(page),
    pageSize: String(cfg.pageSize),
    // Confirmed on the live page. A stable order matters: pages are read one by one.
    sortBy: 'checkInDate',
    sortOrder: 'asc',
  });
  // sortBy / sortOrder (and anything else SiteMinder's own URL carries) go in verbatim.
  if (cfg.extraQuery) for (const [k, v] of new URLSearchParams(cfg.extraQuery)) q.set(k, v);
  return `${cfg.reservationsBase}/${cfg.propertyId}/search?${q}`;
}
