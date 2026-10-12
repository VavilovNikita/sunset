# Deploying the backend

Notes a deploy can't be done safely without. General rules (destructive migrations, the baseline
dump) are in CLAUDE.md, "Migrations".

## Always take a database dump first

Some migrations can't be undone by deploying the previous build. Restoring means restoring a dump,
so take one before every deploy.

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

## Uploads (room photos, floor plans, maintenance photos)

Everything staff upload - room photos, the property, spa and restaurant plans, maintenance task
photos - is a file under `/app/uploads` in the backend container; the database only keeps the
path. `docker-compose.yml` mounts the named volume `uploads` there and pins `UPLOADS_ROOT` to it, so
a normal deploy (`docker compose build backend && docker compose up -d backend`) keeps the files.

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
docker volume ls | grep uploads                      # the volume exists
docker compose exec backend ls /app/uploads          # property-map, spa-map, restaurant-map, rooms, maintenance-tasks
docker compose logs backend | grep "Uploads root"    # the line above, from the last start
```

The Postgres backup in `backup/` does **not** include these files. Back the volume up the same way
you take the dump before a risky deploy, and keep it next to it:

```bash
docker run --rm -v sunset_uploads:/data -v "$PWD":/backup alpine   tar czf /backup/uploads-$(date +%F).tgz -C /data .
# restore into an empty volume:
docker run --rm -v sunset_uploads:/data -v "$PWD":/backup alpine   tar xzf /backup/uploads-YYYY-MM-DD.tgz -C /data
```

If a plan or photo does 404 anyway, upload it again from the admin (Property map, Spa map, the
room's photos) - the screens say so when the image is missing.
