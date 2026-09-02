-- BookingSegment.totalPrice used to be the only price a segment carried, recomputed wholesale
-- from *current* RatePlan/Room.basePrice every time a segment's dates changed (extend, shrink,
-- relocate, undo-relocate) - meaning a rate change made after a booking was placed silently
-- repriced nights the guest had already been quoted, any time staff touched that booking's
-- schedule again. This table gives every night of a segment its own frozen price, snapshotted
-- once when that night first becomes part of the booking and never touched again except by the
-- one explicit, deliberate `POST /bookings/{id}/reprice` action - see BookingWriter's class
-- javadoc for the full write-path story. BookingSegment.totalPrice remains a cached sum of a
-- segment's own rows here (nothing downstream - folio, CSV export, audit summaries - needs to
-- change), not the source of truth anymore.
CREATE TABLE "BookingSegmentNightlyRate" (
    "id"         text PRIMARY KEY,
    "segmentId"  text NOT NULL REFERENCES "BookingSegment"(id) ON DELETE CASCADE,
    "date"       date NOT NULL,
    "price"      numeric(10,2) NOT NULL,
    "createdAt"  timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT "BookingSegmentNightlyRate_segment_date_key" UNIQUE ("segmentId", "date")
);

CREATE INDEX "BookingSegmentNightlyRate_segmentId_idx" ON "BookingSegmentNightlyRate"("segmentId");

-- Backfill: every segment that already exists (created before this table did) gets one row per
-- night, splitting its existing totalPrice evenly - this is a reconstruction, not the real
-- original nightly breakdown (any RatePlan override that applied to specific nights within the
-- segment at booking time was never recorded on its own), but the *sum* is preserved exactly
-- (the rounding remainder is folded entirely onto the segment's last night), and nothing in this
-- app has ever shown a guest or staff member a per-night price - only whole-stay totals - so
-- there is no existing display this could contradict.
WITH nights AS (
    SELECT
        s."id" AS segment_id,
        gs.night::date AS night,
        s."totalPrice",
        COUNT(*) OVER (PARTITION BY s."id") AS night_count,
        ROW_NUMBER() OVER (PARTITION BY s."id" ORDER BY gs.night) AS night_no
    FROM "BookingSegment" s
    CROSS JOIN LATERAL generate_series(s."checkIn", s."checkOut" - INTERVAL '1 day', INTERVAL '1 day') AS gs(night)
),
priced AS (
    SELECT
        segment_id,
        night,
        night_count,
        night_no,
        "totalPrice",
        ROUND("totalPrice" / night_count, 2) AS even_share
    FROM nights
)
INSERT INTO "BookingSegmentNightlyRate" ("id", "segmentId", "date", "price")
SELECT
    gen_random_uuid()::text,
    segment_id,
    night,
    CASE
        WHEN night_no = night_count THEN "totalPrice" - even_share * (night_count - 1)
        ELSE even_share
    END
FROM priced;
