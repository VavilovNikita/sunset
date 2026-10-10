# siteminder-agent

Headless-Chromium (Playwright) job that logs into the SiteMinder extranet, reads the
**Reservations** table and posts each reservation to the backend's
`POST /api/integrations/siteminder/reservations` (see `SiteMinderImportService` and the
operation's description in `openapi.yaml`). Nothing is pushed back to SiteMinder.

Lives in `tools/siteminder-agent/` in this repo, not in the Java backend: it is Node, it has its own
dependencies and release rhythm, and it is deployed as a systemd job beside the backend, not inside the
Docker image. It shares only the HTTP contract.

## What it does and doesn't decide

- **The backend is the idempotency authority.** It keys on `(source=SITEMINDER, reference)` and decides
  create / update / cancel / unchanged / skipped. So the agent posts *every* reservation it sees (including
  modified and cancelled ones), not only "new" ones - filtering to new would lose cancellations and edits.
- `state/seen.json` (reference -> hash of the last accepted payload) only avoids re-posting reservations
  that haven't changed. Failures (400/409/5xx) are not remembered and are retried next run. Deleting the file is harmless.
- **Room type mapping is not here.** The raw SiteMinder name is sent; an unmapped name comes back as 400 on
  `roomTypeName` and is logged verbatim - add it under *SiteMinder room type mappings* (MANAGER+) and the next run picks it up.
- Prices are SiteMinder's total (the one sanctioned exception in CLAUDE.md "Money"). Non-THB amounts are rejected by the backend.

## Setup on the server

```bash
cd /opt/sunsetbeach/tools/siteminder-agent
npm ci                              # or: npm install
npx playwright install --with-deps chromium
cp .env.example .env && chmod 600 .env && $EDITOR .env
```

On the backend set `SITEMINDER_INTEGRATION_KEY` to the same value as `SUNSET_INTEGRATION_KEY` (unset = integration off, every call 401).

### First login / 2FA / captcha

Headless runs fail with a clear message (exit 3) if SiteMinder asks for a code or captcha. Do the first login
headed, on a machine with a display, then copy `state/storage-state.json` (chmod 600) to the server:

```bash
npm run login        # opens a window, fills credentials, waits up to 5 min for you to finish 2FA
```

### Tuning to the real page

I could not see the live extranet while writing this. Columns are matched by **header text** (`src/parse.js`,
`COLUMNS`), so if the table uses other wording the run stops with `LAYOUT CHANGED: ... Headers seen: ...` - add
the wording there. If the page has several tables set `SM_TABLE_SELECTOR`.
`npm run dump` saves `dump/reservations.html` + a screenshot (contains guest data; delete afterwards), and
`npm run dry-run` parses and prints without sending anything.

## Mapping a new room type

Mappings are data (`SiteMinderRoomTypeMapping`, MANAGER+ API), not a migration, and the admin UI has no screen for
them yet. To add one without hand-copying ids (set `SUNSET_STAFF_EMAIL` / `SUNSET_STAFF_PASSWORD` for a MANAGER or ADMIN login first):

```bash
node scripts/add-room-mapping.mjs "Sunset Room with Terrace" "Sunset Terrace Room ABF"
```

It finds the room type by name, creates the mapping, and does nothing if the same mapping already exists. Use the name
exactly as in the Room column but without the `1 x ` prefix (what the import log shows after `roomTypeName`).

## Which reservations are read

The search URL is built on every run (`src/url.js`): `dateType=ModifiedAt`, `fromDate` = today minus
`SM_LOOKBACK_DAYS` (default 3, covers a few missed days), `toDate` = tomorrow, both in hotel time
(Asia/Bangkok), paged with `page` / `pageSize` until an empty page. An empty window is normal
("Looks like there are no reservations" is not an error). To prove parsing on a quiet day:
`node src/run.js --dry-run --lookback=60`.

## Schedule

```bash
sudo cp systemd/siteminder-agent.{service,timer} /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now siteminder-agent.timer
systemctl status siteminder-agent.timer ; systemctl list-timers siteminder-agent.timer
sudo systemctl start siteminder-agent.service    # run once by hand
```

Daily at 02:15 Asia/Bangkok (+ up to 5 min jitter). Paths assume the checkout at `/opt/sunsetbeach` as for `pg-backup`; edit if different.

## Logs and exit codes

`logs/agent.log` and the systemd journal (`journalctl -u siteminder-agent`). Each run ends with a `SUMMARY` line
(found / created / updated / cancelled / unchanged / skipped / duplicates skipped locally / failed / unparseable rows).
No credentials, session data or guest contact details are logged.

| exit | meaning |
|---|---|
| 0 | all good |
| 1 | finished, but some reservations failed or rows couldn't be parsed |
| 2 | missing configuration |
| 3 | login failed (expired/wrong credentials, captcha, 2FA, login form changed) |
| 4 | Reservations page layout changed (no table / columns missing) |
| 5 | unexpected error, or the backend rejected the key (401) |
