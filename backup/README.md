# Database backups

Daily backup of the Postgres database behind the hotel system (bookings,
folios, payments, shifts, staff audit log), stored off the disk the
database itself lives on. If you're here because the server is down, go
straight to [`RESTORE_RUNBOOK.md`](./RESTORE_RUNBOOK.md) instead — this file
is the design rationale, not the emergency procedure.

## 1. Format and tool: `pg_dump`, custom format

`docker exec <postgres-container> pg_dump -U sunsetbeach -d sunsetbeach -Fc`,
run daily by [`pg_backup.sh`](./pg_backup.sh).

Runs inside the existing Postgres container via `docker exec`, so the only
requirement on the host is Docker itself — no Postgres client tools need to
be separately installed or kept in version-sync with the server.

Custom format (`-Fc`) over plain SQL because:

- It's compressed on its own (no separate `gzip` step to remember or get
  wrong).
- `pg_restore --list` shows the full table of contents without touching any
  database — this is what the backup script uses to confirm a dump is real
  and non-empty right after taking it (see "How you'll know it broke"
  below), and it's also the first thing the restore runbook has a
  panicking human do to sanity-check a file before trusting it.
- Restore is `pg_restore --create --clean --if-exists` — one command,
  idempotent (safe to run against a database that may or may not already
  exist), and it restores schema and data together.

The one thing that matters more than the format: **this has to be a full
schema+data dump, not just data.** This database's first Flyway migration
(`V1`) is an intentionally empty placeholder — the actual tables were
created years ago by Prisma, and Flyway only started tracking history from
that point forward. A data-only backup, or "restore data then let
migrations recreate the schema," would not work: migration 1 creates
nothing. `pg_dump`'s default (schema + data together) sidesteps this
entirely — restoring the dump recreates the exact tables as they physically
existed, regardless of what created them originally or whether that
original tooling is still around. This was verified directly, not assumed
— see "Restore verification" below.

## 2. Where backups are stored, and what each option costs and doesn't cover

The rule that matters most: **a copy on the same disk as the database
doesn't protect against a disk failure**, which is the single most likely
failure mode for a one-server, on-prem setup. Everything below is
evaluated against that.

| Option | One-time / recurring cost | Protects against | Does **not** protect against |
|---|---|---|---|
| **External USB drive**, plugged into the same server | ~1,000–3,000฿ one-time for a decent SSD | The database's own disk failing | Theft of the server, fire, flood, or anything that takes out the room the server sits in |
| **NAS on the local network** (small Synology/QNAP, or even a second cheap PC) | ~5,000–15,000฿ one-time, plus it's another device to maintain/patch | Same as above, plus a somewhat more resilient single point (RAID inside the NAS itself if configured) | Same building-level risks as the external drive, unless it physically sits elsewhere (a different room at minimum) |
| **Cloud storage** (e.g. Backblaze B2, AWS S3) | A few cents to low single-digit dollars a month for a database this size (dumps are tens of KB to low hundreds of MB even after months of operation; B2 is roughly $6/TB/month) | Total loss of the building — fire, flood, theft of everything on site | Nothing environmental — but see the personal-data note below, which is the real cost of this option |

**Recommendation for this size of hotel: external drive as the always-on
first line (cheap, works even if the internet is down, catches the most
common failure), plus cloud as the second line specifically for "the
building itself is the problem."** A NAS is a reasonable middle ground if
one is already owned or wanted for other reasons, but it's extra hardware
to maintain for not much more protection than the external drive alone
provides, unless it's kept in a separate room.

`pg_backup.sh` supports all three at once (`BACKUP_DIR`, `OFFSITE_DIR`,
`RCLONE_REMOTE`) — set as many as make sense; each is independent and
optional beyond the first.

### If cloud storage is used: this is a personal-data export, not just a technical decision

The dump contains guests' names, emails, phone numbers, and payment notes.
Uploading it to any third-party cloud service means that data leaves the
hotel's own control — under Thailand's PDPA (and equivalent rules like GDPR
if EU guests are covered by that instead/also), this is a real data
transfer with real obligations, not just an IT implementation detail. In
practice, this means:

1. **The dump must be encrypted before it ever leaves the server**, not
   relying on the provider's own encryption-at-rest. `pg_backup.sh` does
   this with GPG when `GPG_RECIPIENT` is configured — if it's not
   configured but `RCLONE_REMOTE` is, the script uploads in **plaintext**
   and says so loudly in its log, on purpose, so this doesn't happen
   silently.
2. **The decryption private key must not live on the server.** If it did,
   anyone who compromised the server could decrypt the offsite backups too
   — the whole point of the offsite copy is to survive the server being
   compromised or destroyed. Keep the key with the owner/manager, offline
   (a USB stick in a drawer, a password manager, whatever's realistic —
   just not on the machine being backed up).
3. **This is the owner's decision to make knowingly**, not something to
   default into. Whether to use cloud storage at all, and which provider,
   is a data-protection choice about where guest data is allowed to go —
   flag it to them explicitly rather than just picking a vendor.

## 3. Frequency and retention

**Once a day** is enough. Bookings and orders change throughout the day,
but a worst case of "redo today's changes from memory/paper" is a
recoverable inconvenience for hotel staff — it doesn't justify the extra
complexity of intra-day backups (that would mean moving to continuous
WAL archiving for point-in-time recovery, which is a reasonable future
upgrade if the tolerance for lost time ever needs to shrink, but is
meaningfully more machinery than this setup needs today).

**Retention: 14 daily + 8 weekly + 12 monthly generations**, pruned
automatically by `pg_backup.sh`:

- Every backup from the **last 14 days** kept in full — covers "we noticed
  something wrong today that started a few days ago," including the
  explicit week-later scenario this was designed around.
- The **most recent backup of each week** kept for **8 more weeks** beyond
  that (~2 months of week-granularity history).
- The **most recent backup of each month** kept for **12 more months**
  beyond that (~a year of month-granularity history).

So you can always roll back to yesterday, and also to "some Tuesday five
weeks ago" or "sometime last March" if a problem turns out to be older than
it first looked. Storage cost of keeping ~34 generations at once is
negligible — these dumps are small.

## 4. How you'll know it broke

A backup job that silently stops running is worse than no backup job, because
it creates false confidence right up until the day it's needed. Two
independent layers, so this doesn't depend on any one thing working:

1. **Active notification (primary):** `pg_backup.sh` pings a
   [healthchecks.io](https://healthchecks.io)-style URL (`HEALTHCHECK_URL`)
   on every successful run. Configure the check there with a ~27 hour
   expected period; if a day goes by with no ping — because the job didn't
   run, or because Docker was down, or because pg_dump errored, or because
   the verification step below rejected a bad dump — the service emails (or
   texts) automatically. This is the one that matters: it tells you without
   anyone having to go look.
2. **Local fallback (secondary):** [`check_backup_freshness.sh`](./check_backup_freshness.sh)
   checks the newest file in `BACKUP_DIR` and fails loudly if it's missing
   or older than `MAX_AGE_HOURS` (default 30). Doesn't depend on any
   third-party service being reachable or correctly configured — wire it
   into a second systemd timer (`systemd/pg-backup-check.timer`) with its
   own alert (mail, or whatever's already used for other server alerts), or
   just run it by hand periodically.

Beyond "did it run at all": **the backup script also verifies each dump is
real before it's trusted**, not just that `pg_dump` exited 0. It runs
`pg_restore --list` against the fresh dump and checks it lists a sane
number of tables including `Booking` specifically — a truncated, empty, or
otherwise corrupt dump gets treated as a failed run (and pings the failure
endpoint) rather than silently sitting there looking fine until the day
someone needs it.

## 5. Restore verification — actually performed, not just designed

Ran the full cycle end to end against a real copy of this database (the
local dev instance, same schema/migration lineage as production — same
repo, same `flyway_schema_history`, same Prisma-then-Flyway origin):

1. Ran `pg_backup.sh` for real against the running Postgres container —
   produced a dump, and the script's own integrity check passed.
2. Spun up a **brand new, empty** Postgres container (fresh volume, nothing
   carried over).
3. Restored the dump into it with the exact command in the runbook
   (`pg_restore --create --clean --if-exists`).
4. Confirmed via `psql`: all 18 tables present, row counts non-zero in
   `Booking`/`User`/`Order`, and `flyway_schema_history` intact — including
   migration `1`, recorded exactly as `"Pre-Flyway schema (created by
   Prisma migrations)"`, `success = t`.
5. Started the actual Spring Boot application against the restored
   database (nothing else changed — same code, same migration files). The
   startup log showed:
   ```
   Successfully validated 21 migrations
   Current version of schema "public": 20
   Schema "public" is up to date. No migration necessary.
   ```
   i.e. Flyway did not attempt to apply anything, including migration 1 —
   proving the restore doesn't depend on Prisma or the original migration
   history being available anywhere else, only on the dump itself.
6. Hit the running application over HTTP (not just the database directly)
   and got back a real, data-backed response (a login attempt correctly
   rejected with "Invalid email or password" for a real user that exists
   only because the restore put it there) — confirming the whole stack
   works against the restored data, not just that the tables exist.
7. Tore down the test container and volume afterward; the real database was
   never touched by any of this.

This is what "checked the mechanism actually works" means here — not "the
commands should work in theory."
