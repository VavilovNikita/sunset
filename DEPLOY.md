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
