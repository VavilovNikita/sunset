-- A shift code scoped to no area is shared across every area - see ShiftCode's own doc comment
-- (openapi.yaml) for why: only "9" (a split shift in Restaurant/Kitchen, a single shift in Front
-- Office) actually needs one row per area in this hotel's legend. Everything else in the source
-- spreadsheet's legend was being retyped once per area for no reason other than the model
-- requiring it.
ALTER TABLE "ShiftCode" ALTER COLUMN "staffArea" DROP NOT NULL;

-- The existing "ShiftCode_staffArea_code_effectiveFrom_key" unique index already prevents two
-- rows from colliding whenever staffArea is non-null and equal - untouched, still correct.
-- Postgres treats every NULL as distinct from every other NULL in an ordinary unique index
-- though, so that index alone would silently allow two *shared* (staffArea IS NULL) rows for the
-- same code and effectiveFrom - exactly the "which version was the one on a given day" ambiguity
-- V57's own comment explains this index exists to prevent. A second, partial index closes that
-- one gap the first index can't cover, without needing to touch it: casting staffArea to text to
-- fold NULL into a sentinel value (the other way to do this) doesn't work here - Postgres treats
-- an enum-to-text cast as STABLE, not IMMUTABLE (enum labels can be renamed with ALTER TYPE ...
-- RENAME VALUE, so the cast isn't guaranteed constant), and an index expression must be IMMUTABLE.
CREATE UNIQUE INDEX "ShiftCode_shared_code_effectiveFrom_key" ON "ShiftCode" (code, "effectiveFrom") WHERE "staffArea" IS NULL;
