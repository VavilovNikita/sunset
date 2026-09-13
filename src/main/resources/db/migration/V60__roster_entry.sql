-- One employee's shift on one date. A day off is the *absence* of a row here, never a row of
-- its own with some "OFF" marker - the same reasoning the availability model already applies to
-- occupancy (a fact is represented by what's there, not by an extra flag for its opposite).
--
-- UNIQUE(employeeUserId, date) is the concurrency primitive this whole module's editing surface
-- depends on - see CLAUDE.md's Concurrency section for why this is the "at most one" tool, not
-- an exclusion constraint: a date isn't a range two people can overlap on, it's a single atomic
-- slot per person, so a plain unique index is exactly right (same shape as Payment_orderId_key).
-- It's also why the roster swap needs no SERIALIZABLE or deferred-constraint machinery the room
-- and table swaps needed - see RosterService#swapEntries's own comment.
CREATE TABLE "RosterEntry" (
    id                 text PRIMARY KEY,
    "employeeUserId"   text NOT NULL REFERENCES "User"(id),
    date               date NOT NULL,
    "shiftCodeId"      text NOT NULL REFERENCES "ShiftCode"(id),
    note               text,
    locked             boolean NOT NULL DEFAULT false,
    "createdByUserId"  text NOT NULL REFERENCES "User"(id),
    "createdAt"        timestamp(3) NOT NULL DEFAULT now(),
    "updatedAt"        timestamp(3) NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX "RosterEntry_employeeUserId_date_key" ON "RosterEntry" ("employeeUserId", date);
CREATE INDEX "RosterEntry_date_idx" ON "RosterEntry" (date);
