#!/usr/bin/env bash
# Second, independent way to notice a broken backup, for anyone who doesn't
# want to depend on a third-party healthcheck service (or wants both).
#
# Checks that the newest backup in BACKUP_DIR is younger than MAX_AGE_HOURS
# and exits non-zero (with a message on stderr) if not - the last backup is
# missing entirely, or too old. Wire this into whatever you've already got:
#
#   - a second systemd timer (see systemd/pg-backup-check.timer) that emails
#     root on failure (systemd + a working local MTA, or `msmtp`),
#   - a cron line with MAILTO set,
#   - or just run it by hand occasionally - it's cheap and fast.
#
# The healthcheck ping in pg_backup.sh is the primary signal (it tells you
# within a day, actively, without anyone checking anything). This script is
# the fallback that doesn't depend on any external service being reachable
# or configured correctly.
set -euo pipefail

BACKUP_DIR="${BACKUP_DIR:-/mnt/backup-drive/sunsetbeach}"
MAX_AGE_HOURS="${MAX_AGE_HOURS:-30}"

newest="$(find "$BACKUP_DIR" -maxdepth 1 -name 'sunsetbeach_*.dump' -printf '%T@ %p\n' 2>/dev/null | sort -rn | head -1)"

if [ -z "$newest" ]; then
  echo "NO BACKUPS FOUND in ${BACKUP_DIR} - either nothing has ever run, or the drive isn't mounted." >&2
  exit 2
fi

newest_epoch="${newest%% *}"
newest_file="${newest#* }"
now_epoch="$(date +%s)"
age_hours=$(( (now_epoch - ${newest_epoch%.*}) / 3600 ))

if [ "$age_hours" -gt "$MAX_AGE_HOURS" ]; then
  echo "STALE BACKUP: newest is ${newest_file}, ${age_hours}h old (limit ${MAX_AGE_HOURS}h)." >&2
  exit 1
fi

echo "OK: ${newest_file} is ${age_hours}h old."
