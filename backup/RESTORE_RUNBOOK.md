# The server died. Here's what to do.

This is for a real emergency: the hotel server won't boot, its disk is dead,
or the database is visibly corrupted. Read it top to bottom once before
running anything if you can, but every command below is copy-pasteable as-is
(replace anything in `<angle brackets>`).

If you are not sure this is actually an emergency (e.g. "a page looks
wrong"), **stop and don't restore anything yet** — restoring overwrites the
current database. Ask someone who knows the system first if there's any way
to.

This document is deliberately also kept **outside** the git repository (see
the bottom of this file for where) — if the server is dead, the repo that
normally lives on it may not be reachable either.

---

## 1. Where backups live

Three possible places, in order of how likely you are to need them:

1. **External drive plugged into the server** — mounted at
   `/mnt/backup-drive/sunsetbeach`. If the server's main disk died but the
   server box itself (and that external drive) survived, this is where you
   look first.
2. **`<OFFSITE_DIR — fill in during setup, e.g. a NAS path>`** — a second
   local copy, if one was configured.
3. **Cloud storage: `<RCLONE_REMOTE — fill in during setup, e.g. b2:sunsetbeach-backups>`**
   — use this if the server itself is gone (theft, fire, flood) and the
   external drive isn't recoverable either. Files here are named the same
   way as tier 1. If they're `.gpg` files, see step 3.5 below — you need the
   decryption key, which is deliberately **not** on the server (ask
   `<owner/manager name>`).

In every location, files are named:

```
sunsetbeach_YYYYMMDD_HHMMSS.dump           <- the actual backup, use this
sunsetbeach_YYYYMMDD_HHMMSS.globals.sql    <- roles, usually not needed (see step 3)
sunsetbeach_YYYYMMDD_HHMMSS.sha256         <- checksums, use to double check the file isn't corrupted
```

## 2. Pick the right dump

Default choice: **the newest one.** Look at the timestamp in the filename —
it's local server time.

Pick an **older** one instead of the newest only if you have a specific
reason to believe the newest backup(s) already contain bad/corrupted data
(e.g. "we noticed today that a week's worth of bookings look wrong" — in
that case, go back to a dump from before the problem started). This is
exactly why more than one day of history is kept — see the retention table
in `README.md` in this folder if you need to know how far back you can go.

```bash
ls -lt /mnt/backup-drive/sunsetbeach/*.dump | head -5
```

## 3. Restore

These steps assume: Docker is installed on whatever machine you're restoring
to (the repaired server, or a brand new one), and you have the dump file
locally on that machine (copy it there first — `scp`, a USB stick, whatever
works).

### 3.1 (Cloud copy only) Decrypt first

If you had to pull the dump from cloud storage and it's a `.gpg` file:

```bash
gpg --output sunsetbeach_<timestamp>.dump --decrypt sunsetbeach_<timestamp>.dump.gpg
```

This needs the private key that matches `GPG_RECIPIENT` from the backup
config — that key is **not** on the server on purpose. Whoever holds it
(`<owner/manager name>`) needs to either do this step or hand you the key.

### 3.2 Verify the file isn't corrupted

```bash
sha256sum -c sunsetbeach_<timestamp>.sha256
```

Should say `OK` for both files. If it says `FAILED`, don't use this dump —
go back to step 2 and pick a different one (an older one, or the same date
from a different storage tier).

### 3.3 Start a fresh, empty Postgres

```bash
docker volume create sunsetbeach_data
docker run -d --name sunsetbeach-db \
  -e POSTGRES_USER=sunsetbeach -e POSTGRES_PASSWORD=<the real password, from the secrets manager / owner, NOT a guess> \
  -e POSTGRES_DB=postgres \
  -v sunsetbeach_data:/var/lib/postgresql/data \
  -p 5434:5432 \
  postgres:16-alpine
```

Wait a few seconds, then confirm it's up:

```bash
docker exec sunsetbeach-db pg_isready -U sunsetbeach
```

### 3.4 Restore the dump into it

```bash
docker exec -i sunsetbeach-db pg_restore -U sunsetbeach -d postgres --create --clean --if-exists < sunsetbeach_<timestamp>.dump
```

This one command recreates the `sunsetbeach` database from scratch and
loads everything into it — schema and data together. It's normal for this
to take anywhere from a few seconds to a few minutes depending on how much
data there is; it's **not** normal for it to print errors. If it prints
errors, stop and see "If restore itself fails" near the bottom.

You almost never need the `.globals.sql` file — it only matters if extra
database roles/users were created by hand at some point beyond the one
`sunsetbeach` role, which the `postgres:16-alpine` image already recreates
on its own from the `POSTGRES_USER`/`POSTGRES_PASSWORD` above. If you do
need it: `docker exec -i sunsetbeach-db psql -U sunsetbeach -d postgres < sunsetbeach_<timestamp>.globals.sql` — **before** step 3.4, not after.

### 3.5 Check the restore actually worked

```bash
# Tables are all there:
docker exec sunsetbeach-db psql -U sunsetbeach -d sunsetbeach -c '\dt'

# There's actually data in the important ones (numbers will vary - just
# confirm they're not zero):
docker exec sunsetbeach-db psql -U sunsetbeach -d sunsetbeach -c 'SELECT count(*) FROM "Booking";'
docker exec sunsetbeach-db psql -U sunsetbeach -d sunsetbeach -c 'SELECT count(*) FROM "User";'
docker exec sunsetbeach-db psql -U sunsetbeach -d sunsetbeach -c 'SELECT count(*) FROM "Order";'

# The migration history is intact:
docker exec sunsetbeach-db psql -U sunsetbeach -d sunsetbeach -c 'SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank;'
```

The last command should list every migration up through the highest number
that exists in `src/main/resources/db/migration/` in the app's code, all
with `success = t`. **Migration `1` will say "Pre-Flyway schema (created by
Prisma migrations)" and that's correct** — it's an intentionally empty
placeholder migration that predates this database's current schema, which
was originally created by Prisma years ago, not by Flyway. It does nothing
by itself. The tables exist in the restored database because the *dump*
contains them physically, not because any migration created them — this is
exactly why a full `pg_dump` (not just a data-only dump, and not "restore
data then run migrations from scratch") is what gets backed up. If you ever
see someone suggest recovering by running migrations against an empty
database instead of restoring a dump, that will **not** work — migration 1
creates nothing.

### 3.6 Point the application at the restored database and start it

If you're restoring onto the repaired original server, the app's
`DATABASE_URL` should already point at `sunsetbeach-db` (or wherever the
Postgres container is on that host) — just start the app containers/services
as usual.

If you're standing up a **new** server, set `DATABASE_URL` in the app's
`.env` (see `.env.example` in the main repo) to point at the container you
just created, e.g.:

```
DATABASE_URL=jdbc:postgresql://sunsetbeach-db:5432/sunsetbeach
```

(adjust the host/port to match how you started the container above), then
start the backend normally (`docker compose up -d`, or however it's
normally deployed).

**Watch the backend's startup log for these lines:**

```
Successfully validated NN migrations
Current version of schema "public": NN
Schema "public" is up to date. No migration necessary.
```

This is what a healthy restore looks like — Flyway checks the schema
against the code's migration files and finds nothing to do. If instead you
see it *applying* migrations (`Migrating schema "public" to version ...`),
that's fine too **as long as the app version you're running is the same or
newer than the one that made this backup** — see the mismatch section
below. If you see a `FlywayValidateException` or "checksum mismatch," stop —
see "If Flyway complains" below.

### 3.7 Final sanity check

Log in through the actual app (the real login page) with a real staff
account and confirm you can see recent bookings/orders that you recognize.
That's the real proof it worked — everything above is necessary but this is
what actually matters.

---

## 4. If the dump's schema version doesn't match the app's code

Every dump captures `flyway_schema_history` as it was at backup time — a
record of exactly which migrations had been applied. The code you're
running has its own set of migration files. These two won't always match
exactly, and what to do depends on **which one is ahead**:

**Dump is older than the code (the normal case: the code has migrations the
dump doesn't).** This is completely normal — it just means the app has been
updated since that backup was taken. Flyway will apply the missing
migrations automatically and safely on startup, the same way it does every
time the app is deployed after a code update. Nothing special to do; just
watch the startup log to confirm it says `Migrating schema "public" to
version X` for each one and finishes without errors.

**Dump is newer than the code (the dangerous direction: the dump has
migrations the running code doesn't know about).** This means whoever is
about to start the app is running an *older* version of the code than what
produced this backup. **Do not start the app yet.** Flyway will refuse to
run (it won't understand the newer schema), and if it's somehow forced to
run anyway, it can corrupt data. Instead: get the matching or newer version
of the code deployed first (check out the right commit/tag, or pull the
latest from the repo if it's reachable), *then* start the app against the
restored database.

If you genuinely don't know which direction you're in: compare the highest
version number from `SELECT version FROM flyway_schema_history ORDER BY
installed_rank DESC LIMIT 1;` (from step 3.5) against the highest-numbered
file in `src/main/resources/db/migration/` in the code you're about to run.
Dump's number ≤ code's number is safe. Dump's number > code's number is the
dangerous direction above.

## 5. If Flyway complains

- **"Validate failed: Migrations have failed validation... checksum
  mismatch"** — someone edited an already-applied migration file's contents
  after it was released (migrations must never be edited once shipped).
  Don't try to fix this under pressure; get whoever maintains the backend
  code involved before doing anything further.
- **"Schema ... has objects" / won't create baseline** — you likely skipped
  `--clean` or restored into a database that wasn't actually empty. Drop
  the database and redo step 3.3–3.4 rather than trying to patch around it.

## 6. If restore itself fails (step 3.4 prints errors)

- Re-run `sha256sum -c` (step 3.2) again — a half-copied file is the most
  common cause.
- Try the **previous** day's dump (step 2) — if last night's backup was
  itself corrupted (this should have already been caught automatically by
  the backup job's own verification step and alerted, but check).
- If every recent dump has the same problem, go further back using the
  weekly/monthly generations — see the retention table in `README.md`.

---

## 7. Where else this document lives

Copies of this file are kept somewhere that doesn't depend on the server or
the git repo being reachable:

- **https://claude.ai/code/artifact/aedb068f-20e8-4fc3-ab37-5df65a0567fd**
  — this same document, reachable from any device with a browser,
  independent of the hotel's own network or hardware. Bookmark it.
- `<fill in: printed copy taped inside the server cabinet / in the office
  folder marked "IT">`
- `<fill in: who else has an emailed copy>`

If you're reading this from the git repo instead and the server is down,
that's fine too — it means the repo host is up even though the server
isn't, which is one of the reasons there's more than one copy.
