-- ESC/POS network printing: MenuItem gets a print-routing department (separate from the
-- free-text `category` display field), Printer holds the network settings staff configure per
-- department, and PrintJob is a durable queue so a dead/offline printer never blocks the
-- operation that triggered the print (order sent, order closed, shift closed, test page).

CREATE TYPE "MenuDepartment" AS ENUM ('KITCHEN', 'BAR');
ALTER TABLE "MenuItem" ADD COLUMN "department" "MenuDepartment" NOT NULL DEFAULT 'KITCHEN';

-- CASHIER is not a valid MenuItem destination (nothing on the menu prints to the cashier
-- printer - that one only gets guest receipts/pre-bills/Z-reports), hence a separate enum
-- from MenuDepartment rather than reusing one 3-value type for both columns.
CREATE TYPE "PrinterDepartment" AS ENUM ('KITCHEN', 'BAR', 'CASHIER');
CREATE TYPE "PrinterCodepage" AS ENUM ('PC437', 'TIS620');

CREATE TABLE "Printer" (
    id          text PRIMARY KEY,
    name        text NOT NULL,
    department  "PrinterDepartment" NOT NULL,
    host        text NOT NULL,
    port        integer NOT NULL DEFAULT 9100,
    codepage    "PrinterCodepage" NOT NULL DEFAULT 'PC437',
    "isActive"  boolean NOT NULL DEFAULT true,
    "createdAt" timestamp(3) NOT NULL DEFAULT now()
);

-- Keeps routing unambiguous: "the active KITCHEN printer" is always a single lookup, never a
-- list to choose from. A hotel that later needs a second bar printer (e.g. a separate beach
-- bar) can be supported by loosening this - see PrinterService/PrintService comments - without
-- restructuring anything else.
CREATE UNIQUE INDEX "Printer_one_active_per_department" ON "Printer" ("department") WHERE "isActive" = true;

CREATE TYPE "PrintDocumentType" AS ENUM ('KITCHEN_TICKET', 'PREBILL', 'GUEST_RECEIPT', 'Z_REPORT', 'TEST_PAGE');
CREATE TYPE "PrintJobStatus" AS ENUM ('PENDING', 'SENT', 'FAILED');

CREATE TABLE "PrintJob" (
    id             text PRIMARY KEY,
    -- No ON DELETE clause (default RESTRICT): a printer that has ever printed anything can't
    -- be deleted, only deactivated - same "history is immutable" rule as MenuItem/OrderItem.
    "printerId"    text NOT NULL REFERENCES "Printer"(id),
    "documentType" "PrintDocumentType" NOT NULL,
    -- Human-readable one-liner ("Kitchen ticket - Order #ab12cd34", "Z-report - Shift #...")
    -- so staff working the failed-jobs list know what didn't print without decoding raw
    -- ESC/POS bytes.
    summary        text NOT NULL,
    payload        bytea NOT NULL,
    status         "PrintJobStatus" NOT NULL DEFAULT 'PENDING',
    attempts       integer NOT NULL DEFAULT 0,
    "lastError"    text,
    "createdAt"    timestamp(3) NOT NULL DEFAULT now(),
    "updatedAt"    timestamp(3) NOT NULL DEFAULT now()
);

CREATE INDEX "PrintJob_status_idx" ON "PrintJob" ("status");
CREATE INDEX "PrintJob_printerId_idx" ON "PrintJob" ("printerId");
