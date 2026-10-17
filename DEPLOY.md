# Deploying the backend

Notes a deploy can't be done safely without. General rules (destructive migrations, the baseline
dump) are in CLAUDE.md, "Migrations".

## Always take a database dump first

Some migrations can't be undone by deploying the previous build. Restoring means restoring a dump,
so take one before every deploy.

## Settings that belong to one server live in untracked files

A deploy is `git pull` on the VPS. `git pull` refuses to run over a tracked file that was edited
by hand on the server, so anything that is true only for one machine must not be written into a
tracked file such as `docker-compose.yml` - the deploy then fails before it builds anything. Such
settings go in two files git ignores:

- **`.env`** - values: `DATABASE_URL`, secrets, `JAVA_OPTS`, and so on.
- **`docker-compose.override.yml`** (in `.gitignore`) - the shape of the containers. Docker Compose
  reads it automatically next to `docker-compose.yml` and merges it over the tracked file, so
  `docker compose up -d` needs no extra flags.

On the production VPS the one such setting is the network: the backend joins the external Docker
network `sunset-net`, which is how it reaches Postgres. That lives in the override file only
(shape of the file - adapt it to what is on the server):

```yaml
# docker-compose.override.yml - on the VPS only, never committed
services:
  backend:
    networks:
      - default
      - sunset-net

networks:
  sunset-net:
    external: true
```

and `DATABASE_URL` in `.env` uses the database container's name on that network. (If Postgres
runs on the host instead, `DATABASE_URL` uses `host.docker.internal`, which `docker-compose.yml`
already maps; no override is needed.) `docker-compose.yml` no longer carries "uncomment this"
scenarios - never hand-edit it on the server.

If the tracked `docker-compose.yml` is already hand-edited on the server, move that edit into the
override file first, then `git checkout docker-compose.yml`, then pull. Before a deploy, `git status`
on the VPS should list nothing tracked as modified.

**`docker compose config` prints the secrets from `.env`** - database password, JWT secrets,
integration keys - because it expands every variable. Its output must never be pasted into a log, a
chat or an issue. To check that the override is picked up, look at the result on the server
without copying it anywhere, e.g. `docker inspect <backend container> --format '{{json .NetworkSettings.Networks}}'`
after `docker compose up -d`.

## V132: rollback only from a dump once early-departure fees exist

V132 adds `Booking.earlyDepartureFee` and `FolioPayment.shiftId`. It is a pure `ADD COLUMN`, so
deploying the previous build right after it is harmless: the columns sit unused.

**That stops being true once any booking has an early-departure fee** (an early checkout where
staff chose "charge the full booking"). The previous build doesn't know the column, so rolling back
the code alone silently drops the fee from:
- the folio balance: the guest appears to owe less;
- the `PAID` ledger settlement: room revenue is posted short.

Nothing errors, and the numbers are simply wrong. From that point the only safe rollback is
restoring the dump taken before the deploy. Check first:

```sql
SELECT count(*) FROM "Booking" WHERE "earlyDepartureFee" > 0;
```

If the count is 0, a code-only rollback is still safe. If it's above 0, restore the dump.

Deploy sunset-beach together with it: its folio and check-out screens read the fields V132 serves.

## V138: unique room type names

V138 makes a room type's name unique ignoring case and spaces at either end. The migration never
fails on old data: if two types already share a name it creates **no** index and logs one WARNING
per group (`V138: 2 room types are named "deluxe": <id> ("Deluxe"), <id> ("Deluxe ")`), and
`RoomService` still refuses any new duplicate. After deploying, look for the warning in the first
start's log, or ask the database:

```sql
SELECT lower(btrim("name")) AS name, count(*), string_agg("id", ', ') AS ids
FROM "Room" GROUP BY 1 HAVING count(*) > 1;
```

No rows: the index exists, nothing to do. Rows: rename or delete the extra types in the admin (a
type with bookings or units can't be deleted - rename it), then create the index by hand:

```sql
CREATE UNIQUE INDEX "Room_name_normalized_key" ON "Room" (lower(btrim("name")));
```

Rolling back to the previous build is safe: the index is just an index.

## V143: names compared ignoring case, outer and repeated spaces

V143 widens V138: a room type's name and a physical room's label (`RoomUnit.label`) are unique after
lower-casing, trimming and collapsing runs of spaces (also no-break and other Unicode spaces) to one -
"Standard   Room" = "standard room", "Q1" = "q1". Like V138 it never fails on old data: with duplicates it
creates no index and logs one WARNING per group (`V143: 2 rooms are labelled "q1": ...`); the services
still refuse any new duplicate. Check, rename the extras in the admin, then create what was left out:

```sql
SELECT sunset_normalize_name("name") AS name, count(*), string_agg("id", ', ') FROM "Room" GROUP BY 1 HAVING count(*) > 1;
SELECT sunset_normalize_name("label") AS label, count(*), string_agg("id", ', ') FROM "RoomUnit" GROUP BY 1 HAVING count(*) > 1;

DROP INDEX IF EXISTS "Room_name_normalized_key";
CREATE UNIQUE INDEX "Room_name_normalized_key" ON "Room" (sunset_normalize_name("name"));
CREATE UNIQUE INDEX "RoomUnit_label_normalized_key" ON "RoomUnit" (sunset_normalize_name("label"));
```

Rolling back to the previous build is safe: these are only an index and a function.

## Gym equipment (Tuya cloud)

The Gym page (fans on Wi-Fi relays, air conditioners behind IR hubs) talks to the Tuya cloud. It needs three
variables in the backend's `.env` on the VPS - without them the page opens and says the integration is not set up:

| Variable | Where to take it |
|---|---|
| `TUYA_CLIENT_ID` | iot.tuya.com -> Cloud -> Development -> project "sunset" -> Overview -> Authorization Key -> Access ID |
| `TUYA_CLIENT_SECRET` | the same place -> Access Secret |
| `TUYA_API_BASE` | the project's data-centre endpoint, same page (Singapore: `https://openapi-sg.iotbing.com`) |

**Restart the backend after adding or changing them** - they are read at start-up, not per request:
`docker compose up -d backend` (a plain `docker compose restart` does not re-read `.env`). Never paste the values into
a chat, a log or a commit; `docker compose config` prints them, same warning as above.

**The IoT Core trial must be extended by hand.** Tuya's Trial Edition stops working when the subscription lapses and the
page then shows "Tuya integration is unavailable — check the IoT Core subscription". Extend it at iot.tuya.com -> Cloud ->
Project management -> the project -> Extend trial period. The trial also allows about 26 000 API calls a month and stops
until the month ends when they run out; nothing polls, the Gym page reads state only when it is opened or refreshed, and
Settings -> Gym shows this month's call count (Bangkok month).

**Air conditioners need one more API on the project.** Switching a relay works with the base subscription; controlling an
air conditioner and listing an IR hub's remotes call the infrared APIs, which Tuya answers with "This API is not subscribed"
(code 28841101) until that service is added to the project: iot.tuya.com -> Cloud -> Development -> project -> Service API ->
the IR control hub service -> subscribe (authorize the project). The Gym page reports it in plain words.

### Gym reservations and the equipment automation

Guests (checked-in, from their account on the site) and the front desk (`/admin/gym/schedule`) reserve the gym; the
equipment follows the reservation by itself. Nothing to configure beyond the Tuya keys above - the defaults are the
owner's - but know how it behaves when something is wrong.

**What it does.** `GymAutomationService` runs once a minute (`app.gym.automation.interval-ms`, default 60000). For
every unfinished reservation it looks for two moments: *switch-on* `leadMinutes` (default 30) before the start, and
*switch-off* at the end (or right after a cancellation, if the equipment had been switched on). At switch-on every
active device that takes part in bookings (Settings -> Gym -> "Switched by gym reservations", on by default for every
device) goes on: fans via their relay, air conditioners with ONE scene command (power 1, mode 0 = cool, 16 degrees,
wind 3 = strongest; ADMIN can change mode/temperature/fan and the lead time under Settings -> Gym). At switch-off
everything goes off, **except** when another reservation starts within the lead time (back-to-back sessions never
switch the equipment off in between, and the second session sends nothing at all at its own switch-on).

**What it never does.** It never reads a device's state and never polls - it spends Tuya quota (about 26 000 calls a
month on the trial) only at those two moments: roughly (devices x 2) calls per reservation. The manual Gym page keeps
working and the automation does not fight it: a fan someone switches off by hand mid-session stays off until the next
transition.

**Restarts and gaps.** The memory is the database (`GymReservationAction`: one row per reservation, device and phase),
not the process. A restart sends nothing twice. A switch-on that was due while the backend was down is sent when it
comes back *if the session has not ended yet*; if it has ended, nothing is switched on (the reservation shows "missed").
A switch-off is always sent. Rarely, a crash between Tuya accepting a command and the row being saved repeats that one
command - harmless, on/off and the scene are idempotent.

**Failures.** One device failing does not stop the others. A failed command is retried on the next minute's run, up
to 3 attempts in all (`app.gym.automation.max-attempts`, pause `app.gym.automation.retry-pause-seconds`), then the
device is marked failed. Every phase ends with one audit entry written by the system (Audit log -> entity "Gym
reservation", action "Gym automation result") naming what went on and what did not, and the schedule shows it on the
reservation: all fine / partly (with the devices that failed and Tuya's reason) / failed. Where to look when the
equipment did not react: 1. the reservation's panel on `/admin/gym/schedule`; 2. the audit log entry above; 3. the
backend log (`docker compose logs backend | grep -i "gym automation"`), which also says why Tuya refused. An unreachable
Tuya shows as "Tuya is not responding"; an expired trial as "Tuya integration is unavailable" (see above).

**Air conditioners need the IR service.** Exactly as for the manual Air 1 / Air 2 controls: the scene command goes
through the IR APIs, which Tuya answers with "This API is not subscribed" (code 28841101) until **IR Control Hub** is
subscribed in the Tuya project (iot.tuya.com -> Cloud -> Development -> project -> Service API -> IR Control Hub ->
subscribe and authorize the project). Until then the fans still switch and the air conditioners are reported as failed
on every reservation. This has not been verified live yet (the subscription was missing when this was built).

**Migrations V141/V142** only add tables and enum values (no destructive step), and V142 adds `GymDevice.inBookings`
(default true for every existing device). The exclusion constraint `gym_reservation_no_overlap` needs no extension
beyond `btree_gist`, which is already installed for the spa.

Migrations V139 (tables) and V140 (audit values) are additive; there is nothing to restore on rollback.

## Lighting (WLED controllers)

The Lighting page talks to WLED controllers on the hotel LAN over plain HTTP (`/json/info`, `/json/state`, `/presets.json`).
The backend runs on the VPS, so - exactly like the ESC/POS printers and the fingerprint terminal - it reaches them **only
through the wg-hotel WireGuard tunnel**:

1. **The tunnel must route the subnet the controllers are on.** On the VPS the `AllowedIPs` of the hotel peer in the wg-hotel
   config must contain that subnet (for example `192.168.1.0/24`), and the hotel-side router/peer must route it back. If the
   printers already work, their subnet is probably the same one - check that the controllers' addresses are inside it.
2. **Give every controller a fixed address** (a DHCP reservation in the hotel router, or a static IP in WLED's Wi-Fi settings).
   The page stores an address, not a MAC.
3. **Check reachability from the backend container** before adding a controller in Settings -> Lighting:
   ```bash
   docker compose exec backend curl -s -m 3 http://<controller-ip>/json/info
   ```
   It must print a JSON object with `"ver"` and `"leds"`. If `curl` is missing in the image, run it on the VPS host
   instead (`curl -s -m 3 http://<controller-ip>/json/info`) - but a host that can reach it while the container cannot
   points at docker's network/forwarding rules, not at the tunnel. No output or a timeout: look at `wg show` (a recent
   handshake) and at the tunnel's `AllowedIPs`.
4. **Set `LIGHTING_ALLOWED_CIDRS` (required).** Put the hotel LAN the controllers are on into the backend's `.env`, as CIDR
   (comma-separated for several; see `.env.example` for the variable, use your own subnet), then
   `docker compose up -d backend` (a plain restart does not re-read `.env`). **Without it lighting is off**: the Lighting
   pages say "not configured" and the server sends nothing to any controller. That is deliberate - the other containers on
   the compose network (the database, the frontend) are in 172.16/12 as well, so "any private address" would let an admin
   point a controller at the database. Pick a range that does **not** contain docker's networks (`docker network inspect`
   shows them). A malformed value stops the backend from starting, on purpose.
5. Optional timeouts: `LIGHTING_CONNECT_TIMEOUT_MS` (2000), `LIGHTING_REQUEST_TIMEOUT_MS` (2500), `LIGHTING_STATE_CACHE_SECONDS` (10).

The address is checked when it is saved and again on every request, so a controller whose DNS name or address later
points somewhere not allowed simply shows as unreachable. Nothing polls the controllers; one that is switched off at the
wall shows "offline" until someone refreshes the page. WLED has no password on its JSON API - the tunnel is what keeps it
private, so do not forward a controller's port to the internet.

Migrations V146 (table) and V147 (audit values) only add things; rolling back to the previous build is safe.

## Staff notifications (Telegram)

Late / absence / terminal / room-block messages to staff, over a Telegram bot. The server **polls** Telegram
(`getUpdates`) - there is no webhook, nothing public listens, no nginx rule and no firewall change; the VPS only needs
to reach `api.telegram.org` over HTTPS (outbound).

1. **Create the bot** in Telegram with @BotFather (`/newbot`) and copy the token. Treat it like a password: it is in
   every request URL to Telegram, so the backend never logs a URL, a request or an exception message of that client.
2. **Put it in the backend's `.env` on the VPS**: `TELEGRAM_BOT_TOKEN=<token>`, then `docker compose up -d backend`
   (a plain restart does not re-read `.env`). Unset or empty means **Telegram is off**: nothing is polled or sent,
   `GET /notifications/status` says `telegramConfigured:false` and the admin pages show "Telegram is not configured".
   `docker-compose.yml` needs no change (`.env` is already the backend's `env_file`).
3. **Check it came up**: the backend log has no `Telegram ... failed` lines, and Settings -> Notifications no longer shows
   the banner (the bot's name appears within a few seconds - the server asks `getMe` once and keeps the answer).
4. **After the deploy, in the admin** (ADMIN): Settings -> Notifications - choose the people who hear about lateness
   ("late recipients") and the **director recipients** (absences, a silent terminal, blocks nobody on shift can take),
   check the thresholds, quiet hours (22:00-07:00 by default) and the morning reminder time (09:00).
   Then, for each employee (MANAGER+): Link Telegram -> show the QR / send the link -> the employee opens it and presses
   **Start** (the bot answers "Linked: name"). An employee without a link still gets a row in the log, marked
   "No channel". Set an employee's language there too (EN / TH / RU; the settings hold the default).
5. Optional (defaults in brackets): `TELEGRAM_POLL_INTERVAL_MS` [3000], `TELEGRAM_LONG_POLL_SECONDS` [1],
   `NOTIFICATIONS_SCAN_INTERVAL_MS` [60000], `NOTIFICATIONS_DELIVERY_INTERVAL_MS` [5000], `NOTIFICATIONS_MAX_ATTEMPTS` [5].

Things that look wrong but are not:

- **The late grace cannot be saved below the terminal's poll interval** (5 minutes with the default
  `app.attendance.device-poll-interval-ms`): a punch cannot be seen sooner.
- **No late / absence message is sent while an attendance terminal has not answered for the silence threshold (15 minutes
  by default), or when no terminal is active.** The directors get one "terminal not responding" and, later, one "back
  online". That is the guard against telling someone they are late because the terminal was off.
- **Messages raised during quiet hours are written at once (the log shows them as pending) and go out when quiet hours
  end**, each as its own message.
- The Thai texts are a first translation and need a native speaker's review (they live in `NotificationMessages`).
- A person who blocks the bot is unlinked automatically (the log shows "The recipient blocked the bot"); linking again
  is a new code. Deactivating an employee removes their link.
- A message is the text as it was worded when raised; changing an employee's language later does not rewrite old ones.

Migrations V148 (tables, columns, the settings row) and V149 (audit values) only add things; rolling back to the previous
build is safe - the new tables are simply unused. To switch the whole feature off without a rollback, unset
`TELEGRAM_BOT_TOKEN`.

## Uploads (room photos, floor plans, maintenance photos)

Everything staff upload - room photos, the property, spa and restaurant plans, maintenance task
photos - is a file under `/app/uploads` in the backend container; the database only keeps the
path. `docker-compose.yml` mounts the named volume `uploads` at `/app/uploads`, which is where
`UPLOADS_ROOT` points by default (the container's working directory is `/app` and the default is
`./uploads`) - so `.env` must **not** set `UPLOADS_ROOT` to anything else, or the files land in the
container's own filesystem and are gone after the next `up -d --build`. A normal deploy
(`docker compose build backend && docker compose up -d backend`) keeps the files.

What loses them (the database then still points at images that 404, and the maps show the
"image not found" notice):

- `docker compose down -v`, `docker volume prune` or `docker system prune --volumes` - never run these on the VPS;
- the checkout directory being renamed or cloned again: the volume is named after the compose
  project (`sunset_uploads` for `~/sunset/sunset`), a new project name starts an empty one;
- running the backend outside Docker from another directory: `UPLOADS_ROOT` defaults to `./uploads`,
  relative to wherever it was started. On start the backend logs the absolute path it uses and how
  many files it found (`Uploads root ... holds N file(s)`, or a WARN when it is missing or empty).

Check it on the VPS after a deploy:

```bash
grep UPLOADS_ROOT .env                                # prints nothing (or /app/uploads) - never another path
docker volume ls | grep uploads                      # the volume exists
docker compose exec backend ls /app/uploads          # property-map, spa-map, restaurant-map, rooms, maintenance-tasks
docker compose logs backend | grep "Uploads root"    # the line above, from the last start
```

The Postgres backup (`backup/pg_backup.sh`) does **not** include these files. They have their own
daily backup, installed next to it - see below.

### Uploads backup: install (once, by hand on the VPS)

`backup/uploads_backup.sh` archives the volume once a day to `/var/backups/sunsetbeach/uploads_YYYYMMDD_HHMMSS.tgz`
(+ a `.sha256`), reads the archive back before trusting it, mirrors it to `OFFSITE_DIR` / `RCLONE_REMOTE`
if those are set for the database backup, and rotates like the dumps (14 daily, 8 weekly, 12 monthly
- the same `KEEP_*`). It refuses to rotate anything if the volume suddenly holds no files while the
previous archive did (a recreated, empty volume is exactly the incident this is for), and refuses if
the volume does not exist. The `pg-backup` setup is assumed: scripts under `/opt/sunsetbeach/backup`,
settings in `/etc/sunsetbeach-backup.env`.

```bash
# 1. scripts (from the checkout; pg_backup.sh is already there)
sudo cp ~/sunset/sunset/backup/uploads_backup.sh ~/sunset/sunset/backup/check_uploads_backup_freshness.sh /opt/sunsetbeach/backup/
sudo chmod +x /opt/sunsetbeach/backup/uploads_backup.sh /opt/sunsetbeach/backup/check_uploads_backup_freshness.sh

# 2. settings: append the "Uploads volume backup" block of backup/pg_backup.env.example to the env file,
#    and make UPLOADS_VOLUME the real name from:  docker volume ls | grep uploads
sudoedit /etc/sunsetbeach-backup.env
sudo mkdir -p /var/backups/sunsetbeach && sudo chmod 700 /var/backups/sunsetbeach

# 3. systemd units: backup at 03:50 (after the database's 03:30), freshness check at 12:05
sudo cp ~/sunset/sunset/backup/systemd/uploads-backup.{service,timer} ~/sunset/sunset/backup/systemd/uploads-backup-check.{service,timer} /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now uploads-backup.timer uploads-backup-check.timer

# 4. run it once now and look at the result
sudo systemctl start uploads-backup.service
journalctl -u uploads-backup.service -n 20 --no-pager      # "wrote uploads_...tgz: N file(s)"
ls -l /var/backups/sunsetbeach
systemctl list-timers 'uploads-*'
```

After a script update, copy the script again (step 1); the units only change if their files do.
A failed night shows as a failed `uploads-backup.service`, a stale or missing archive as a failed
`uploads-backup-check.service`, and - if `UPLOADS_HEALTHCHECK_URL` is set - as a missing ping.

### Uploads backup: restore

```bash
cd /var/backups/sunsetbeach
sha256sum -c uploads_YYYYMMDD_HHMMSS.sha256          # the archive is intact
tar -tzf uploads_YYYYMMDD_HHMMSS.tgz | head          # looks like rooms/, spa-map/, ...
# a GPG copy from the cloud first: gpg --decrypt uploads_....tgz.gpg > uploads_....tgz  (key held offline)
cd ~/sunset/sunset && docker compose stop backend
docker run --rm -v sunset_uploads:/data -v /var/backups/sunsetbeach:/backup:ro --entrypoint tar sunset-backend:latest   -xzf /backup/uploads_YYYYMMDD_HHMMSS.tgz -C /data
docker compose up -d backend && docker compose logs backend | grep "Uploads root"   # holds N file(s)
```

Restoring into a volume that still has files overwrites files of the same name and leaves the rest.
Before a risky deploy, take an extra archive by hand: `sudo systemctl start uploads-backup.service`.

If a plan or photo does 404 anyway, upload it again from the admin (Property map, Spa map, the
room's photos) - the screens say so when the image is missing.
