-- Creates one RoomUnit per existing Room.quantity unit, so every room type ends up with exactly
-- as many active units as it previously claimed via `quantity` (see the RoomUnit_label_key
-- unique index added in V8 - this insert must never violate it).
--
-- Label = "<room name> <n>" (n = 1..quantity), which is meaningful without any manual input -
-- staff are expected to rename these to real room numbers ("203", "Garden Bungalow 4", ...)
-- through the RoomUnit UI afterward. Room.name has no uniqueness constraint, so two room types
-- could in principle share a name; disambiguating that case requires knowing how many *distinct
-- rooms* share a name, not how many *unit rows* would be generated for that name (the latter is
-- inflated by quantity and would wrongly trigger disambiguation for any single room with
-- quantity > 1) - room_name_counts below counts rooms, not units, to avoid that.
WITH room_name_counts AS (
    SELECT name, COUNT(*) AS room_count
    FROM "Room"
    GROUP BY name
),
per_room_units AS (
    SELECT r.id AS room_id, r.name, gs.n, c.room_count
    FROM "Room" r
    JOIN room_name_counts c ON c.name = r.name
    CROSS JOIN LATERAL generate_series(1, r.quantity) AS gs(n)
)
INSERT INTO "RoomUnit" (id, "roomId", label, "isActive", "createdAt")
SELECT
    gen_random_uuid()::text,
    room_id,
    CASE
        WHEN room_count > 1 THEN name || ' ' || n || ' (' || substr(room_id, 1, 6) || ')'
        ELSE name || ' ' || n
    END,
    true,
    now()
FROM per_room_units;
