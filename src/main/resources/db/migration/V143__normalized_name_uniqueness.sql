-- Names that compare equal after normalisation are one name: lower case, trimmed, and every run of
-- spaces inside collapsed to one ("Standard   Room" = "standard room"; also no-break and other
-- Unicode spaces). V138 did this for room types with lower(btrim(name)), which still told
-- "Standard  Room" from "Standard Room"; physical rooms (RoomUnit.label) were unique only exactly
-- ("Q1" and "q1" could both exist).
--
-- Like V138, this never fails on old data (a failed migration stops the backend from starting): a
-- unique index is created only when the table has no duplicates under the new rule. Otherwise each
-- group is reported as a WARNING in the Flyway log and the index is left out (room types keep the
-- weaker V138 index in that case); RoomService / RoomUnitService still refuse a new duplicate. Fix
-- the duplicates, then create the index by hand - the statements are in DEPLOY.md.

CREATE OR REPLACE FUNCTION sunset_normalize_name(value TEXT) RETURNS TEXT
    LANGUAGE sql IMMUTABLE PARALLEL SAFE
AS $$
    SELECT lower(btrim(regexp_replace(value, E'[ \t\r\n\f\u000b\u00a0\u2000-\u200b\u2028\u2029\u202f\u205f\u3000\ufeff]+', ' ', 'g')))
$$;

DO $$
DECLARE
    dup RECORD;
    rooms_dup BOOLEAN := FALSE;
    units_dup BOOLEAN := FALSE;
BEGIN
    FOR dup IN
        SELECT sunset_normalize_name("name") AS normalized, count(*) AS copies,
               string_agg("id" || ' ("' || "name" || '")', ', ' ORDER BY "id") AS rows_found
        FROM "Room"
        GROUP BY sunset_normalize_name("name")
        HAVING count(*) > 1
    LOOP
        rooms_dup := TRUE;
        RAISE WARNING 'V143: % room types are named "%": %', dup.copies, dup.normalized, dup.rows_found;
    END LOOP;

    IF rooms_dup THEN
        RAISE WARNING 'V143: unique index "Room_name_normalized_key" NOT replaced because of the duplicates above - fix them, then run the statements in DEPLOY.md (V143).';
    ELSE
        DROP INDEX IF EXISTS "Room_name_normalized_key";
        CREATE UNIQUE INDEX "Room_name_normalized_key" ON "Room" (sunset_normalize_name("name"));
    END IF;

    FOR dup IN
        SELECT sunset_normalize_name("label") AS normalized, count(*) AS copies,
               string_agg("id" || ' ("' || "label" || '")', ', ' ORDER BY "id") AS rows_found
        FROM "RoomUnit"
        GROUP BY sunset_normalize_name("label")
        HAVING count(*) > 1
    LOOP
        units_dup := TRUE;
        RAISE WARNING 'V143: % rooms are labelled "%": %', dup.copies, dup.normalized, dup.rows_found;
    END LOOP;

    IF units_dup THEN
        RAISE WARNING 'V143: unique index "RoomUnit_label_normalized_key" NOT created because of the duplicates above - fix them, then run the statements in DEPLOY.md (V143).';
    ELSE
        CREATE UNIQUE INDEX "RoomUnit_label_normalized_key" ON "RoomUnit" (sunset_normalize_name("label"));
    END IF;
END
$$;
