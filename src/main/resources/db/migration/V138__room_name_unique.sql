-- A room type's name is unique, ignoring case and spaces at either end ("Deluxe" = " deluxe ").
-- The Prisma-era schema never had the rule, so the reference list could hold two "Deluxe" rows and
-- a guest or a receptionist couldn't tell them apart. RoomService checks it on create/update
-- (409 with a readable message); this index is what holds under a race.
--
-- Data that already breaks the rule must not break the migration (a failed V138 stops the
-- backend from starting at all): the index is created only when there are no duplicates. When
-- there are, each group is reported as a WARNING in the Flyway log, the index is left out, and
-- RoomService still refuses any new duplicate. Rename or delete the extra types, then create the
-- index by hand - the statement is in DEPLOY.md ("V138: unique room type names").

DO $$
DECLARE
    dup RECORD;
    found BOOLEAN := FALSE;
BEGIN
    FOR dup IN
        SELECT lower(btrim("name")) AS normalized, count(*) AS copies, string_agg("id" || ' ("' || "name" || '")', ', ' ORDER BY "id") AS rooms
        FROM "Room"
        GROUP BY lower(btrim("name"))
        HAVING count(*) > 1
    LOOP
        found := TRUE;
        RAISE WARNING 'V138: % room types are named "%": %', dup.copies, dup.normalized, dup.rooms;
    END LOOP;

    IF found THEN
        RAISE WARNING 'V138: unique index "Room_name_normalized_key" NOT created because of the duplicates above - fix them, then create it (see DEPLOY.md).';
    ELSE
        CREATE UNIQUE INDEX "Room_name_normalized_key" ON "Room" (lower(btrim("name")));
    END IF;
END
$$;
