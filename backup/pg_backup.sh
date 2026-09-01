#!/usr/bin/env bash
# Daily backup of the Sunset Beach Postgres database.
#
# Runs entirely through `docker exec` against the running Postgres container,
# so the only requirement on the host is Docker itself - no Postgres client
# tools need to be installed on the server. Produces one pg_dump (custom
# format, schema+data) and one pg_dumpall --globals-only per run, verifies
# the dump is a well-formed, non-empty archive before it's trusted, copies it
# to whichever offsite destinations are configured, prunes old generations
# on a daily/weekly/monthly schedule, and pings a healthcheck URL so a silent
# failure gets noticed without anyone having to go look.
#
# Configure via environment variables, normally set in /etc/sunsetbeach-backup.env
# and loaded by the systemd unit (see systemd/pg-backup.service). See
# pg_backup.env.example for what each variable means and a starting point.
set -euo pipefail

# ---- configuration (env vars, with defaults matching the repo's compose files) ----
PG_CONTAINER="${PG_CONTAINER:-sunset-beach-db-1}"
PG_DB="${PG_DB:-sunsetbeach}"
PG_USER="${PG_USER:-sunsetbeach}"

# Tier 1: local backup directory. MUST be a different physical disk/volume
# than the one Postgres's own data lives on - a copy on the same disk does
# not survive the disk failing, which is the scenario this exists for.
BACKUP_DIR="${BACKUP_DIR:-/mnt/backup-drive/sunsetbeach}"

# Tier 2a (optional): a second local-ish path to mirror into - typically a
# mounted NAS share. Anything cp -a can write to. Leave empty to skip.
OFFSITE_DIR="${OFFSITE_DIR:-}"

# Tier 2b (optional): an rclone remote (e.g. "b2:sunsetbeach-backups" or
# "s3:sunsetbeach-backups") to mirror into for offsite/cloud protection.
# Leave empty to skip. Requires `rclone` installed and configured
# (`rclone config`) separately - this script does not manage rclone auth.
RCLONE_REMOTE="${RCLONE_REMOTE:-}"

# If set, the copy sent to RCLONE_REMOTE is GPG-encrypted first (the dump
# contains guest personal data - see RESTORE_RUNBOOK.md for why this
# matters). Value is a GPG recipient (key ID or email) whose *public* key is
# already imported on this host. The private key must NOT live on this
# server - it should be held by the owner/manager, offline, so a compromised
# server can't decrypt its own offsite backups.
GPG_RECIPIENT="${GPG_RECIPIENT:-}"

# healthchecks.io-style ping URL (or any URL that accepts a bare GET and,
# separately, GET/POST to "<url>/fail"). Free tier at healthchecks.io is
# enough. Leave empty to skip - but see check_backup_freshness.sh, which is
# the fallback if you don't want a third-party dependency.
HEALTHCHECK_URL="${HEALTHCHECK_URL:-}"

# Retention: every daily backup for KEEP_DAILY days, then the most recent
# backup of each ISO week for KEEP_WEEKLY weeks beyond that, then the most
# recent backup of each calendar month for KEEP_MONTHLY months beyond that.
KEEP_DAILY="${KEEP_DAILY:-14}"
KEEP_WEEKLY="${KEEP_WEEKLY:-8}"
KEEP_MONTHLY="${KEEP_MONTHLY:-12}"

LOG_FILE="${LOG_FILE:-/var/log/sunsetbeach-backup.log}"
LAST_SUCCESS_FILE="${LAST_SUCCESS_FILE:-${BACKUP_DIR}/.last_success}"

# ---- plumbing ----
ts() { date '+%Y-%m-%d %H:%M:%S'; }
log() { echo "[$(ts)] $*" | tee -a "$LOG_FILE" >&2; }

on_failure() {
  local exit_code=$?
  log "FAILED (exit $exit_code). See above for details."
  if [ -n "$HEALTHCHECK_URL" ]; then
    curl -fsS --retry 3 -m 15 --data-binary "backup failed, exit $exit_code, see $LOG_FILE on the server" \
      "${HEALTHCHECK_URL}/fail" >/dev/null 2>&1 || log "also failed to notify ${HEALTHCHECK_URL}/fail"
  fi
  exit "$exit_code"
}
trap on_failure ERR

RUN_ID="$(date +%Y%m%d_%H%M%S)"
DUMP_NAME="sunsetbeach_${RUN_ID}.dump"
GLOBALS_NAME="sunsetbeach_${RUN_ID}.globals.sql"
SUM_NAME="sunsetbeach_${RUN_ID}.sha256"

mkdir -p "$BACKUP_DIR"
log "=== backup run ${RUN_ID} starting ==="

# ---- 1. take the dump ----
# -Fc: custom format. Compressed on its own, restorable with a single
# `pg_restore` call, and its table of contents can be listed without
# extracting anything (`pg_restore --list`) - useful for the sanity check
# below and for a panicking human to confirm a dump is real before trusting it.
log "dumping ${PG_DB} from container ${PG_CONTAINER}..."
docker exec "$PG_CONTAINER" pg_dump -U "$PG_USER" -d "$PG_DB" -Fc > "${BACKUP_DIR}/${DUMP_NAME}"

# Roles/grants aren't part of a per-database dump. This DB's role is
# recreated by the Postgres image itself from POSTGRES_USER/POSTGRES_PASSWORD
# on a fresh container, so this is cheap belt-and-suspenders in case anyone
# ever adds extra roles by hand.
docker exec "$PG_CONTAINER" pg_dumpall -U "$PG_USER" --globals-only > "${BACKUP_DIR}/${GLOBALS_NAME}"

# ---- 2. verify it's a real, non-empty, well-formed dump before trusting it ----
# A pg_dump that "succeeded" but produced a truncated/empty/corrupt archive is
# worse than an obvious failure, because it looks fine until the day someone
# needs it. `pg_restore --list` parses the archive's table of contents
# without touching any database - if the file is broken, this fails loudly.
log "verifying dump integrity..."
listing="$(docker exec -i "$PG_CONTAINER" pg_restore --list < "${BACKUP_DIR}/${DUMP_NAME}")"
table_count="$(echo "$listing" | grep -c 'TABLE DATA' || true)"
if [ "$table_count" -lt 5 ]; then
  log "REFUSING to trust this dump: pg_restore --list found only ${table_count} table(s) of data (expected several - Booking, Order, User, etc.). Treating this as a failed run."
  exit 1
fi
if ! echo "$listing" | grep -q 'TABLE DATA public Booking '; then
  log "REFUSING to trust this dump: the Booking table is missing from the archive listing."
  exit 1
fi
log "dump looks sane: ${table_count} tables with data, including Booking."

sha256sum "${BACKUP_DIR}/${DUMP_NAME}" "${BACKUP_DIR}/${GLOBALS_NAME}" > "${BACKUP_DIR}/${SUM_NAME}"
log "wrote ${DUMP_NAME} ($(du -h "${BACKUP_DIR}/${DUMP_NAME}" | cut -f1))"

# ---- 3. mirror to offsite destinations ----
if [ -n "$OFFSITE_DIR" ]; then
  log "mirroring to OFFSITE_DIR=${OFFSITE_DIR}..."
  mkdir -p "$OFFSITE_DIR"
  cp -a "${BACKUP_DIR}/${DUMP_NAME}" "${BACKUP_DIR}/${GLOBALS_NAME}" "${BACKUP_DIR}/${SUM_NAME}" "$OFFSITE_DIR/"
fi

if [ -n "$RCLONE_REMOTE" ]; then
  log "mirroring to RCLONE_REMOTE=${RCLONE_REMOTE}..."
  if [ -n "$GPG_RECIPIENT" ]; then
    gpg --batch --yes --trust-model always -r "$GPG_RECIPIENT" \
      --output "${BACKUP_DIR}/${DUMP_NAME}.gpg" --encrypt "${BACKUP_DIR}/${DUMP_NAME}"
    rclone copy "${BACKUP_DIR}/${DUMP_NAME}.gpg" "${BACKUP_DIR}/${GLOBALS_NAME}" "${BACKUP_DIR}/${SUM_NAME}" "$RCLONE_REMOTE"
    rm -f "${BACKUP_DIR}/${DUMP_NAME}.gpg"
  else
    log "WARNING: RCLONE_REMOTE is set but GPG_RECIPIENT is not - the dump contains guest personal data and will be uploaded in PLAINTEXT. See RESTORE_RUNBOOK.md."
    rclone copy "${BACKUP_DIR}/${DUMP_NAME}" "${BACKUP_DIR}/${GLOBALS_NAME}" "${BACKUP_DIR}/${SUM_NAME}" "$RCLONE_REMOTE"
  fi
fi

# ---- 4. prune old generations (daily/weekly/monthly), same rule everywhere ----
# Keeps: every backup from the last KEEP_DAILY days; beyond that, the newest
# backup in each ISO week for KEEP_WEEKLY weeks; beyond that, the newest
# backup in each calendar month for KEEP_MONTHLY months; deletes the rest.
prune_dir() {
  local dir="$1"
  [ -d "$dir" ] || return 0
  local now_epoch; now_epoch="$(date +%s)"
  local daily_cutoff=$((now_epoch - KEEP_DAILY * 86400))
  local weekly_cutoff=$((now_epoch - (KEEP_DAILY + KEEP_WEEKLY * 7) * 86400))
  local monthly_cutoff=$((now_epoch - (KEEP_DAILY + KEEP_WEEKLY * 7 + KEEP_MONTHLY * 31) * 86400))

  declare -A seen_week=()
  declare -A seen_month=()

  # Newest first, so the *first* file seen in a given week/month bucket is
  # the most recent one - that's the one we keep.
  local f base stamp file_epoch week_key month_key
  for f in $(find "$dir" -maxdepth 1 -name 'sunsetbeach_*.dump' | sort -r); do
    base="$(basename "$f")"
    stamp="${base#sunsetbeach_}"; stamp="${stamp%.dump}"   # YYYYMMDD_HHMMSS
    file_epoch="$(date -d "${stamp:0:8} ${stamp:9:2}:${stamp:11:2}:${stamp:13:2}" +%s 2>/dev/null || echo "$now_epoch")"

    local keep=0
    if [ "$file_epoch" -ge "$daily_cutoff" ]; then
      keep=1
    elif [ "$file_epoch" -ge "$weekly_cutoff" ]; then
      week_key="$(date -d "@${file_epoch}" +%G-%V)"
      if [ -z "${seen_week[$week_key]+x}" ]; then seen_week[$week_key]=1; keep=1; fi
    elif [ "$file_epoch" -ge "$monthly_cutoff" ]; then
      month_key="$(date -d "@${file_epoch}" +%Y-%m)"
      if [ -z "${seen_month[$month_key]+x}" ]; then seen_month[$month_key]=1; keep=1; fi
    fi

    if [ "$keep" -eq 0 ]; then
      log "pruning ${f} (and matching .globals.sql/.sha256/.gpg) from ${dir}"
      rm -f "$f" "${dir}/sunsetbeach_${stamp}.globals.sql" "${dir}/sunsetbeach_${stamp}.sha256" "${dir}/sunsetbeach_${stamp}.dump.gpg"
    fi
  done
}

prune_dir "$BACKUP_DIR"
[ -n "$OFFSITE_DIR" ] && prune_dir "$OFFSITE_DIR"
# RCLONE_REMOTE pruning is intentionally not automated here: deleting from a
# cloud remote by script is exactly the kind of mistake that turns a backup
# system into the incident. Prune it manually/periodically, or wire up
# `rclone delete` deliberately once you've watched a few backup cycles land.

echo "$(ts) OK ${DUMP_NAME}" > "$LAST_SUCCESS_FILE"

if [ -n "$HEALTHCHECK_URL" ]; then
  curl -fsS --retry 3 -m 15 "$HEALTHCHECK_URL" >/dev/null 2>&1 || log "WARNING: dump succeeded but the healthcheck ping itself failed - check network/URL."
fi

log "=== backup run ${RUN_ID} OK ==="
