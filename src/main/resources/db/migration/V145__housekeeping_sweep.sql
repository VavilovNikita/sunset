-- One row per hotel day on which the vacated-room housekeeping job (rooms released by a planned
-- room move taking effect) has run: the row is the job's claim on the day, so a repeat run - the
-- hourly catch-up after a restart, a second instance - does nothing and never re-dirties a room that
-- housekeeping cleaned in the meantime.
CREATE TABLE "HousekeepingSweep" (
    "day"   date PRIMARY KEY,
    "ranAt" timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
