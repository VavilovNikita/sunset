-- When the printer was last tried for this job. updatedAt also moves on a dismissal, so it cannot say this.
-- NULL = never tried (and for jobs older than this column, whose attempt time was not kept).
ALTER TABLE "PrintJob" ADD COLUMN "lastAttemptAt" timestamp(3);
