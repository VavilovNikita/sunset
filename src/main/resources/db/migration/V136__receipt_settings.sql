-- What the guest receipt and the pre-bill look like on paper (GET/PUT /settings/receipt,
-- ADMIN-only like the rest of /settings/**). Kitchen/bar tickets and the Z report are working
-- documents for staff and are not designable.
--
-- Same singleton shape as "VatSettings" (V118) and "LifecycleEmailSettings" (V112): fixed primary
-- key of 1, pinned by a CHECK, seeded here, only ever updated. The seeded defaults reproduce what
-- the printer produced before this table existed - 80mm paper, Font A, bold centered title, no
-- header/footer/logo, every optional line shown - so nothing changes on paper until an admin edits.
--
-- Enumerated text is TEXT + CHECK (see CLAUDE.md, Migrations), sizes are 1 or 2 (ESC/POS only has
-- multiples of the base glyph, not arbitrary point sizes), and the logo is the normalized
-- grayscale PNG itself in a bytea column: it is a few KB, belongs to this one row, and a file
-- on disk would be one more thing to back up and keep in step with the row.
CREATE TABLE "ReceiptSettings" (
    id                   integer PRIMARY KEY CHECK (id = 1),
    "paperWidth"         text    NOT NULL DEFAULT 'MM80' CHECK ("paperWidth" IN ('MM58', 'MM80')),
    "bodyFont"           text    NOT NULL DEFAULT 'A' CHECK ("bodyFont" IN ('A', 'B')),
    "receiptTitle"       text    NOT NULL DEFAULT 'GUEST RECEIPT' CHECK (length("receiptTitle") BETWEEN 1 AND 24),
    "prebillTitle"       text    NOT NULL DEFAULT 'PRE-BILL' CHECK (length("prebillTitle") BETWEEN 1 AND 24),
    "titleSize"          integer NOT NULL DEFAULT 1 CHECK ("titleSize" IN (1, 2)),
    "totalSize"          integer NOT NULL DEFAULT 1 CHECK ("totalSize" IN (1, 2)),
    "totalBold"          boolean NOT NULL DEFAULT false,
    "headerText"         text    NOT NULL DEFAULT '',
    "headerBold"         boolean NOT NULL DEFAULT true,
    "footerText"         text    NOT NULL DEFAULT '',
    "showOrderNumber"    boolean NOT NULL DEFAULT true,
    "showTime"           boolean NOT NULL DEFAULT true,
    "showVatLine"        boolean NOT NULL DEFAULT true,
    "showPaymentMethod"  boolean NOT NULL DEFAULT true,
    "showLogo"           boolean NOT NULL DEFAULT false,
    "logoWidthPercent"   integer NOT NULL DEFAULT 60 CHECK ("logoWidthPercent" BETWEEN 20 AND 100),
    "logoPng"            bytea,
    "updatedAt"          timestamp(3) NOT NULL DEFAULT now()
);

INSERT INTO "ReceiptSettings" (id) VALUES (1);
