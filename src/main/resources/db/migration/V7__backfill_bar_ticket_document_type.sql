-- Backfills existing PrintJob rows that were tagged KITCHEN_TICKET but are actually bar tickets.
-- OrderPrintingService.printTickets always wrote the station name into `summary` ("Kitchen
-- ticket — Order #…" vs. "Bar ticket — Order #…"), so a prefix match is a reliable and
-- sufficiently precise way to identify them - print-job volume is low enough that this one-time
-- backfill doesn't need to be more sophisticated than that.
UPDATE "PrintJob"
SET "documentType" = 'BAR_TICKET'
WHERE "documentType" = 'KITCHEN_TICKET'
  AND "summary" LIKE 'Bar ticket%';
