#!/usr/bin/env bash
# Same fallback check as check_backup_freshness.sh, for the uploads archives: exits non-zero when the
# newest uploads_*.tgz in UPLOADS_BACKUP_DIR is missing or older than MAX_AGE_HOURS. Wired to
# systemd/uploads-backup-check.timer; independent of any third-party healthcheck service.
set -euo pipefail

UPLOADS_BACKUP_DIR="${UPLOADS_BACKUP_DIR:-/var/backups/sunsetbeach}"
MAX_AGE_HOURS="${MAX_AGE_HOURS:-30}"

newest="$(find "$UPLOADS_BACKUP_DIR" -maxdepth 1 -name 'uploads_*.tgz' -printf '%T@ %p\n' 2>/dev/null | sort -rn | head -1)"

if [ -z "$newest" ]; then
  echo "NO UPLOADS BACKUPS FOUND in ${UPLOADS_BACKUP_DIR} - either nothing has ever run, or the directory is not where uploads_backup.sh writes." >&2
  exit 2
fi

newest_epoch="${newest%% *}"
newest_file="${newest#* }"
now_epoch="$(date +%s)"
age_hours=$(( (now_epoch - ${newest_epoch%.*}) / 3600 ))

if [ "$age_hours" -gt "$MAX_AGE_HOURS" ]; then
  echo "STALE UPLOADS BACKUP: newest is ${newest_file}, ${age_hours}h old (limit ${MAX_AGE_HOURS}h)." >&2
  exit 1
fi

echo "OK: ${newest_file} is ${age_hours}h old."
