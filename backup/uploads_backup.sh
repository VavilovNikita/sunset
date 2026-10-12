#!/usr/bin/env bash
# Daily backup of the uploads volume: room photos, the property/spa/restaurant floor plans and
# maintenance-task photos. The database only keeps the *paths* of these files, so the Postgres dump
# (pg_backup.sh) cannot bring them back - without this, losing the Docker volume leaves every map
# and photo as a 404 until someone uploads it again.
#
# Same shape as pg_backup.sh and configured by the same env file (/etc/sunsetbeach-backup.env, see
# pg_backup.env.example): one verified archive per run, optional mirrors, daily/weekly/monthly
# rotation, a healthcheck ping. Runs through `docker run` against the named volume, so the host
# needs nothing but Docker (the backend image already has `tar`, so nothing has to be pulled).
#
# The archive is written under a .partial name and renamed only once it has been read back, so the
# freshness check and the rotation never see a half-written file.
set -euo pipefail

# ---- configuration (env vars) ----
# `docker volume ls | grep uploads` - the name is "<compose project>_uploads"; for the checkout in
# ~/sunset/sunset the project is "sunset".
UPLOADS_VOLUME="${UPLOADS_VOLUME:-sunset_uploads}"
# Any image that has tar. The backend image is already on the host.
UPLOADS_TAR_IMAGE="${UPLOADS_TAR_IMAGE:-sunset-backend:latest}"
UPLOADS_BACKUP_DIR="${UPLOADS_BACKUP_DIR:-/var/backups/sunsetbeach}"

# Mirrors and retention are shared with pg_backup.sh on purpose: one set of numbers to know.
OFFSITE_DIR="${OFFSITE_DIR:-}"
RCLONE_REMOTE="${RCLONE_REMOTE:-}"
GPG_RECIPIENT="${GPG_RECIPIENT:-}"
KEEP_DAILY="${KEEP_DAILY:-14}"
KEEP_WEEKLY="${KEEP_WEEKLY:-8}"
KEEP_MONTHLY="${KEEP_MONTHLY:-12}"

# Its own dead-man's-switch check, separate from the database's (they are different jobs).
UPLOADS_HEALTHCHECK_URL="${UPLOADS_HEALTHCHECK_URL:-}"
UPLOADS_LOG_FILE="${UPLOADS_LOG_FILE:-/var/log/sunsetbeach-uploads-backup.log}"
LAST_SUCCESS_FILE="${UPLOADS_LAST_SUCCESS_FILE:-${UPLOADS_BACKUP_DIR}/.uploads_last_success}"

# ---- plumbing ----
ts() { date '+%Y-%m-%d %H:%M:%S'; }
log() { echo "[$(ts)] $*" | tee -a "$UPLOADS_LOG_FILE" >&2; }

on_failure() {
  local exit_code=$?
  log "FAILED (exit $exit_code). See above for details."
  rm -f "${PARTIAL:-}" 2>/dev/null || true
  if [ -n "$UPLOADS_HEALTHCHECK_URL" ]; then
    curl -fsS --retry 3 -m 15 --data-binary "uploads backup failed, exit $exit_code, see $UPLOADS_LOG_FILE on the server" \
      "${UPLOADS_HEALTHCHECK_URL}/fail" >/dev/null 2>&1 || log "also failed to notify ${UPLOADS_HEALTHCHECK_URL}/fail"
  fi
  exit "$exit_code"
}
trap on_failure ERR

# Files (not directory entries) in an archive; a corrupt previous archive counts as empty.
count_files_lenient() { { tar -tzf "$1" 2>/dev/null || true; } | { grep -vc '/$' || true; }; }

RUN_ID="$(date +%Y%m%d_%H%M%S)"
ARCHIVE_NAME="uploads_${RUN_ID}.tgz"
SUM_NAME="uploads_${RUN_ID}.sha256"
PARTIAL="${UPLOADS_BACKUP_DIR}/${ARCHIVE_NAME}.partial"

umask 077
mkdir -p "$UPLOADS_BACKUP_DIR"
log "=== uploads backup run ${RUN_ID} starting ==="

# ---- 1. the volume must exist - docker run would silently create an empty one otherwise ----
if ! docker volume inspect "$UPLOADS_VOLUME" >/dev/null 2>&1; then
  log "REFUSING: Docker volume '${UPLOADS_VOLUME}' does not exist (docker run would invent an empty one and 'back up' nothing). Find the real name with: docker volume ls | grep uploads"
  false  # fires the ERR trap: logs "FAILED" and pings the healthcheck (a bare `exit` would not)
fi

# ---- 2. archive it (volume mounted read-only) ----
log "archiving volume ${UPLOADS_VOLUME}..."
docker run --rm -v "${UPLOADS_VOLUME}:/data:ro" --entrypoint tar "$UPLOADS_TAR_IMAGE" -czf - -C /data . > "$PARTIAL"

# ---- 3. read it back: a truncated archive that "succeeded" is worse than a failure ----
log "verifying archive..."
# At top level on purpose: if tar cannot read the archive, the ERR trap fails the run.
LISTING="$(mktemp)"
tar -tzf "$PARTIAL" > "$LISTING"
files="$(grep -vc '/$' "$LISTING" || true)"
rm -f "$LISTING"
previous="$(find "$UPLOADS_BACKUP_DIR" -maxdepth 1 -name 'uploads_*.tgz' | sort | tail -1)"
if [ "$files" -eq 0 ]; then
  previous_files=0
  [ -n "$previous" ] && previous_files="$(count_files_lenient "$previous")"
  if [ "$previous_files" -gt 0 ]; then
    # The volume was lost and recreated empty (or emptied): the exact incident this exists for.
    # Do not let an empty archive become "the latest backup" and, over the weeks, push out the real ones.
    log "REFUSING: the volume holds no files, but the previous backup ($(basename "$previous")) has ${previous_files}. Not rotating anything. Is the volume the right one / was it recreated?"
    false  # fires the ERR trap: logs "FAILED" and pings the healthcheck (a bare `exit` would not)
  fi
  log "WARNING: the volume holds no files (nothing has been uploaded yet?). Archive kept."
fi
mv "$PARTIAL" "${UPLOADS_BACKUP_DIR}/${ARCHIVE_NAME}"
( cd "$UPLOADS_BACKUP_DIR" && sha256sum "$ARCHIVE_NAME" > "$SUM_NAME" )
log "wrote ${ARCHIVE_NAME}: ${files} file(s), $(du -h "${UPLOADS_BACKUP_DIR}/${ARCHIVE_NAME}" | cut -f1)"

# ---- 4. mirror to offsite destinations (same variables as the database backup) ----
if [ -n "$OFFSITE_DIR" ]; then
  log "mirroring to OFFSITE_DIR=${OFFSITE_DIR}..."
  mkdir -p "$OFFSITE_DIR"
  cp -a "${UPLOADS_BACKUP_DIR}/${ARCHIVE_NAME}" "${UPLOADS_BACKUP_DIR}/${SUM_NAME}" "$OFFSITE_DIR/"
fi
if [ -n "$RCLONE_REMOTE" ]; then
  log "mirroring to RCLONE_REMOTE=${RCLONE_REMOTE}..."
  if [ -n "$GPG_RECIPIENT" ]; then
    gpg --batch --yes --trust-model always -r "$GPG_RECIPIENT" \
      --output "${UPLOADS_BACKUP_DIR}/${ARCHIVE_NAME}.gpg" --encrypt "${UPLOADS_BACKUP_DIR}/${ARCHIVE_NAME}"
    rclone copy "${UPLOADS_BACKUP_DIR}/${ARCHIVE_NAME}.gpg" "${UPLOADS_BACKUP_DIR}/${SUM_NAME}" "$RCLONE_REMOTE"
    rm -f "${UPLOADS_BACKUP_DIR}/${ARCHIVE_NAME}.gpg"
  else
    rclone copy "${UPLOADS_BACKUP_DIR}/${ARCHIVE_NAME}" "${UPLOADS_BACKUP_DIR}/${SUM_NAME}" "$RCLONE_REMOTE"
  fi
fi

# ---- 5. prune old generations - the same daily/weekly/monthly rule as pg_backup.sh ----
prune_dir() {
  local dir="$1"
  [ -d "$dir" ] || return 0
  local now_epoch; now_epoch="$(date +%s)"
  local daily_cutoff=$((now_epoch - KEEP_DAILY * 86400))
  local weekly_cutoff=$((now_epoch - (KEEP_DAILY + KEEP_WEEKLY * 7) * 86400))
  local monthly_cutoff=$((now_epoch - (KEEP_DAILY + KEEP_WEEKLY * 7 + KEEP_MONTHLY * 31) * 86400))

  declare -A seen_week=()
  declare -A seen_month=()

  # Newest first, so the first file seen in a week/month bucket is the one kept.
  local f base stamp file_epoch week_key month_key keep
  for f in $(find "$dir" -maxdepth 1 -name 'uploads_*.tgz' | sort -r); do
    base="$(basename "$f")"
    stamp="${base#uploads_}"; stamp="${stamp%.tgz}"   # YYYYMMDD_HHMMSS
    file_epoch="$(date -d "${stamp:0:8} ${stamp:9:2}:${stamp:11:2}:${stamp:13:2}" +%s 2>/dev/null || echo "$now_epoch")"

    keep=0
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
      log "pruning ${f} (and its .sha256) from ${dir}"
      rm -f "$f" "${dir}/uploads_${stamp}.sha256"
    fi
  done
}

prune_dir "$UPLOADS_BACKUP_DIR"
[ -n "$OFFSITE_DIR" ] && prune_dir "$OFFSITE_DIR"
# RCLONE_REMOTE is not pruned by script, for the same reason as in pg_backup.sh.

echo "$(ts) OK ${ARCHIVE_NAME}" > "$LAST_SUCCESS_FILE"

if [ -n "$UPLOADS_HEALTHCHECK_URL" ]; then
  curl -fsS --retry 3 -m 15 "$UPLOADS_HEALTHCHECK_URL" >/dev/null 2>&1 || log "WARNING: backup succeeded but the healthcheck ping itself failed - check network/URL."
fi

log "=== uploads backup run ${RUN_ID} OK ==="
