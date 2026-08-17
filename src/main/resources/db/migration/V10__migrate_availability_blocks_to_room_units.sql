-- Migrates existing Availability.blockedCount rows into RoomUnitBlock. A legacy row only ever
-- said "N units of this room type are off-sale on this date" - it never recorded *which* N, so
-- that detail cannot be recovered here. This backfill is intentionally kept rather than dropped:
-- discarding it would silently make already-blocked dates (e.g. a room genuinely under
-- renovation) sellable again the moment this migration runs, which is a real operational risk,
-- not just a loss of reporting precision.
--
-- For each (roomId, date, blockedCount=N) it deterministically picks the first N active
-- RoomUnit rows of that room type - ordered by "createdAt", id, i.e. the "Unit 1..N" order V9
-- created them in - and blocks each of them for that single day. The specific unit chosen is
-- arbitrary and may not be the physical room actually intended; the reason text says so
-- explicitly so staff know to verify/reassign rather than trust it at face value.
INSERT INTO "RoomUnitBlock" (id, "roomUnitId", "fromDate", "toDate", reason, "createdAt")
SELECT
    gen_random_uuid()::text,
    picked.id,
    a.date,
    a.date,
    'Auto-migrated from legacy block count — room assignment is arbitrary, please verify.',
    now()
FROM "Availability" a
JOIN LATERAL (
    SELECT id
    FROM "RoomUnit"
    WHERE "roomId" = a."roomId" AND "isActive" = true
    ORDER BY "createdAt", id
    LIMIT a."blockedCount"
) picked ON true
WHERE a."blockedCount" > 0;
