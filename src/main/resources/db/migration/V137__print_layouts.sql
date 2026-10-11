-- One layout per printed document - guest receipt, pre-bill, kitchen ticket, bar ticket, Z-report
-- (GET/PUT /settings/print-layouts/{documentType}, ADMIN-only like the rest of /settings/**).
-- V136's single "ReceiptSettings" row covered only the receipt and the pre-bill, which shared one
-- layout and differed by title; the kitchen and bar wanted their own (a large item font, no logo),
-- so the unit is now the document, and each carries its own paper width (a document goes to one
-- department's printer, and the kitchen's roll needn't match the till's).
--
-- One table, one row per document, every column present for every document: which of them a given
-- document actually uses (a kitchen ticket has no VAT line, a Z-report has no items) is the
-- renderer's and the editor's business, so a layout can't drift into a state a document-specific
-- table would have forbidden. The CHECK pins the five document types (TEXT + CHECK, see CLAUDE.md,
-- Migrations). The seeded defaults reproduce what each document printed before it was
-- configurable, so nothing changes on paper until an admin edits.
--
-- The logo is shared (one hotel, one mark) and lives in its own singleton row.
--
-- "ReceiptSettings" is left in place, unread, after its row is copied below: dropping it is
-- destructive (see CLAUDE.md, Migrations) and buys nothing until this has run in production; a
-- later migration removes it.
CREATE TABLE "PrintLayout" (
    "documentType"     text    PRIMARY KEY CHECK ("documentType" IN ('RECEIPT', 'PREBILL', 'KITCHEN_TICKET', 'BAR_TICKET', 'Z_REPORT')),
    "paperWidth"       text    NOT NULL DEFAULT 'MM80' CHECK ("paperWidth" IN ('MM58', 'MM80')),
    "bodyFont"         text    NOT NULL DEFAULT 'A' CHECK ("bodyFont" IN ('A', 'B')),
    "title"            text    NOT NULL CHECK (length("title") BETWEEN 1 AND 24),
    "titleSize"        integer NOT NULL DEFAULT 1 CHECK ("titleSize" IN (1, 2)),
    "headerText"       text    NOT NULL DEFAULT '',
    "headerBold"       boolean NOT NULL DEFAULT true,
    "footerText"       text    NOT NULL DEFAULT '',
    "showLogo"         boolean NOT NULL DEFAULT false,
    "logoWidthPercent" integer NOT NULL DEFAULT 60 CHECK ("logoWidthPercent" BETWEEN 20 AND 100),
    "showTime"         boolean NOT NULL DEFAULT true,
    "itemSize"         integer NOT NULL DEFAULT 1 CHECK ("itemSize" IN (1, 2)),
    "showLocation"     boolean NOT NULL DEFAULT true,
    "locationSize"     integer NOT NULL DEFAULT 1 CHECK ("locationSize" IN (1, 2)),
    "showOrderNumber"  boolean NOT NULL DEFAULT true,
    "showWaiter"       boolean NOT NULL DEFAULT true,
    "showItemNotes"    boolean NOT NULL DEFAULT true,
    "showVatLine"      boolean NOT NULL DEFAULT true,
    "showPaymentMethod" boolean NOT NULL DEFAULT true,
    "totalSize"        integer NOT NULL DEFAULT 1 CHECK ("totalSize" IN (1, 2)),
    "totalBold"        boolean NOT NULL DEFAULT false,
    "updatedAt"        timestamp(3) NOT NULL DEFAULT now()
);

CREATE TABLE "PrintLogo" (
    id          integer PRIMARY KEY CHECK (id = 1),
    "logoPng"   bytea,
    "updatedAt" timestamp(3) NOT NULL DEFAULT now()
);

-- Whatever an admin already set on the receipt and pre-bill carries over (V136 was live briefly).
INSERT INTO "PrintLayout" ("documentType", "paperWidth", "bodyFont", "title", "titleSize", "headerText", "headerBold",
                           "footerText", "showLogo", "logoWidthPercent", "showTime", "showOrderNumber", "showVatLine",
                           "showPaymentMethod", "totalSize", "totalBold")
SELECT 'RECEIPT', "paperWidth", "bodyFont", "receiptTitle", "titleSize", "headerText", "headerBold", "footerText",
       "showLogo", "logoWidthPercent", "showTime", "showOrderNumber", "showVatLine", "showPaymentMethod", "totalSize", "totalBold"
FROM "ReceiptSettings" WHERE id = 1;

INSERT INTO "PrintLayout" ("documentType", "paperWidth", "bodyFont", "title", "titleSize", "headerText", "headerBold",
                           "footerText", "showLogo", "logoWidthPercent", "showTime", "showOrderNumber", "showVatLine",
                           "showPaymentMethod", "totalSize", "totalBold")
SELECT 'PREBILL', "paperWidth", "bodyFont", "prebillTitle", "titleSize", "headerText", "headerBold", "footerText",
       "showLogo", "logoWidthPercent", "showTime", "showOrderNumber", "showVatLine", "showPaymentMethod", "totalSize", "totalBold"
FROM "ReceiptSettings" WHERE id = 1;

INSERT INTO "PrintLayout" ("documentType", "title") VALUES
    ('KITCHEN_TICKET', 'KITCHEN TICKET'),
    ('BAR_TICKET', 'BAR TICKET'),
    ('Z_REPORT', 'Z-REPORT');

-- A database that somehow had no ReceiptSettings row (it is seeded by V136, so this is belt and braces).
INSERT INTO "PrintLayout" ("documentType", "title") SELECT 'RECEIPT', 'GUEST RECEIPT' WHERE NOT EXISTS (SELECT 1 FROM "PrintLayout" WHERE "documentType" = 'RECEIPT');
INSERT INTO "PrintLayout" ("documentType", "title") SELECT 'PREBILL', 'PRE-BILL' WHERE NOT EXISTS (SELECT 1 FROM "PrintLayout" WHERE "documentType" = 'PREBILL');

INSERT INTO "PrintLogo" (id, "logoPng") SELECT 1, "logoPng" FROM "ReceiptSettings" WHERE id = 1;
INSERT INTO "PrintLogo" (id) SELECT 1 WHERE NOT EXISTS (SELECT 1 FROM "PrintLogo");
