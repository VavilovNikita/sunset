-- The one VAT rate GET /reports/revenue-statistic (Z410) computes with (GET/PUT /settings/vat,
-- ADMIN-only like the rest of /settings/**).
--
-- Same singleton shape as "LifecycleEmailSettings" (V112): a fixed primary key of 1, pinned by a
-- CHECK so a second row can never exist, seeded here, only ever updated. A table of its own
-- rather than a column on that one: the two have nothing in common but being settings, and a
-- generic key-value table for one number would be more machinery than the value needs.
--
-- 7.00 is Thailand's standard rate - an editable starting value, not a fallback the report
-- carries in code. Percent, two decimals; 0 is allowed (VAT not charged), 100 is not.
CREATE TABLE "VatSettings" (
    id          integer PRIMARY KEY CHECK (id = 1),
    "vatRate"   numeric(5, 2) NOT NULL DEFAULT 7.00 CHECK ("vatRate" >= 0 AND "vatRate" < 100),
    "updatedAt" timestamp(3) NOT NULL DEFAULT now()
);

INSERT INTO "VatSettings" (id) VALUES (1);
