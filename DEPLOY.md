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

and `DATABASE_URL` in `.env` uses the database container's name on that network. The `networks:`
comments inside `docker-compose.yml` (Scenario A/B) that say "uncomment this" describe the same
thing as a hand-edit of the tracked file; use the override file instead.

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
