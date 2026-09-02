-- Lets staff close a FAILED print job that will never usefully retry (printer replaced, the order
-- was already handed to the guest, the kitchen was told verbally) without deleting the row -
-- PrintJob includes guest receipts and Z-reports, whose history has to survive. Nullable:
-- "dismissedAt" null is the default and by far the common case ("not dismissed"); the three
-- columns are set together, never independently.
ALTER TABLE "PrintJob" ADD COLUMN "dismissedAt" TIMESTAMP(3);
ALTER TABLE "PrintJob" ADD COLUMN "dismissedByUserId" TEXT REFERENCES "User"(id);
ALTER TABLE "PrintJob" ADD COLUMN "dismissNote" TEXT;
