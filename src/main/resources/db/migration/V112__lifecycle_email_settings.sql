-- Admin-configurable switches and thresholds for LifecycleEmailService's daily sweep
-- (GET/PUT /settings/lifecycle-emails, ADMIN-only).
--
-- Singleton shape: a fixed primary key of 1, pinned by a CHECK so a second row can never exist,
-- seeded here. Readers load id 1 and nothing else; there is no create or delete, only update.
--
-- Every type starts disabled: deploying this must not start emailing guests before an admin has
-- looked at the settings and chosen to. The numbers are only starting suggestions.
CREATE TABLE "LifecycleEmailSettings" (
    id                          integer PRIMARY KEY CHECK (id = 1),
    "preArrivalEnabled"         boolean NOT NULL DEFAULT false,
    "preArrivalDaysBefore"      integer NOT NULL DEFAULT 3 CHECK ("preArrivalDaysBefore" >= 0),
    "postStayEnabled"           boolean NOT NULL DEFAULT false,
    "postStayDaysAfter"         integer NOT NULL DEFAULT 1 CHECK ("postStayDaysAfter" >= 0),
    "postStayReviewUrl"         text,
    "winBackEnabled"            boolean NOT NULL DEFAULT false,
    "winBackMonthsSinceStay"    integer NOT NULL DEFAULT 12 CHECK ("winBackMonthsSinceStay" >= 1),
    "updatedAt"                 timestamp(3) NOT NULL DEFAULT now()
);

INSERT INTO "LifecycleEmailSettings" (id) VALUES (1);
