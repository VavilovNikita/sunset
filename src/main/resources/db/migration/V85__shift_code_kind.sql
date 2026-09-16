-- Names what a shift code actually is, instead of every caller re-deriving it from
-- countsAsWorked/isPaid/interval shape (or, worse, from the code's own text - the roster grid's
-- daily totals used to match code == "PH" literally to count absences, which silently breaks the
-- moment "PH" is retyped to anything else). Nullable and not backfilled here: a sensible default
-- can be guessed from each existing code's own shape (see ShiftCodeService#computeSuggestedKind),
-- but a guess is not a fact - it's shown for confirmation via PATCH /shift-codes/{id}/kind, not
-- silently applied by this migration. Every code created from here on requires one at creation
-- (see ShiftCodeCreateInput).
CREATE TYPE "ShiftCodeKind" AS ENUM ('MORNING', 'SPLIT', 'EVENING', 'OPEN_SCHEDULE', 'ABSENCE');

ALTER TABLE "ShiftCode" ADD COLUMN "kind" "ShiftCodeKind";
