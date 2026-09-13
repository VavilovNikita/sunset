CREATE TYPE "PunchDirection" AS ENUM ('IN', 'OUT');
-- SCANNER is unused until a real punch clock exists - see this table's own comment for why
-- adding one later needs no schema change, only a second writer of this same table.
CREATE TYPE "PunchSource" AS ENUM ('MANUAL', 'SCANNER');

-- A raw clock-in/out stream, not paired sessions - a split shift's four punches are simply four
-- rows here, and pairing consecutive IN/OUT punches into worked intervals happens at read time
-- (the planned-vs-actual comparison), never at write time. This is deliberate: nothing on the
-- write path needs to know in advance whether a day is a single shift, a split, or OP, and an
-- odd punch count (a missed clock-out, which will happen constantly) is simply left as what it
-- is - an incomplete day - rather than guessed at. It's closed only by recording another punch
-- through this same table with an explanatory note, never by silently inventing a timestamp.
--
-- recordedByUserId is null for a SCANNER-sourced punch (the device/badge is the source of
-- truth, there's no staff member to attribute it to) and required for MANUAL.
CREATE TABLE "AttendancePunch" (
    id                 text PRIMARY KEY,
    "employeeUserId"   text NOT NULL REFERENCES "User"(id),
    "punchAt"          timestamp(3) NOT NULL,
    direction          "PunchDirection" NOT NULL,
    source             "PunchSource" NOT NULL,
    "recordedByUserId" text REFERENCES "User"(id),
    note               text,
    "createdAt"        timestamp(3) NOT NULL DEFAULT now(),
    CONSTRAINT attendance_punch_manual_needs_recorder CHECK (source <> 'MANUAL' OR "recordedByUserId" IS NOT NULL)
);

CREATE INDEX "AttendancePunch_employeeUserId_punchAt_idx" ON "AttendancePunch" ("employeeUserId", "punchAt");
