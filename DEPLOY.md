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
