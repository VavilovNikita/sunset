-- POS module: menu, tables, orders/line items, cashier shifts, payments.
--
-- "Table" is a SQL keyword, so the physical table backing the Table API schema is named
-- "PosTable" instead (see TableEntity) — the REST contract/DTO name is unaffected, this is
-- purely a DB-side naming choice made ahead of time to avoid living with a keyword-quoted
-- table name everywhere.

CREATE TYPE "Zone" AS ENUM ('RESTAURANT', 'BAR', 'SPA', 'POOL', 'ROOM_SERVICE');
CREATE TYPE "OrderStatus" AS ENUM ('OPEN', 'SENT', 'PAID', 'CANCELLED');
CREATE TYPE "PaymentMethod" AS ENUM ('CASH', 'CARD', 'ROOM_CHARGE', 'OTHER');
CREATE TYPE "ShiftStatus" AS ENUM ('OPEN', 'CLOSED');

CREATE TABLE "MenuItem" (
    id            text PRIMARY KEY,
    name          text NOT NULL,
    description   text NOT NULL,
    category      text NOT NULL,
    price         numeric(10,2) NOT NULL,
    "isAvailable" boolean NOT NULL DEFAULT true,
    "createdAt"   timestamp(3) NOT NULL DEFAULT now()
);

CREATE TABLE "PosTable" (
    id         text PRIMARY KEY,
    zone       "Zone" NOT NULL,
    label      text NOT NULL,
    capacity   integer NOT NULL,
    "isActive" boolean NOT NULL DEFAULT true
);

CREATE TABLE "Order" (
    id               text PRIMARY KEY,
    -- ON DELETE SET NULL (not RESTRICT): a table can be deleted once it has no OPEN/SENT
    -- orders left (enforced in TableService before the delete), even though closed/cancelled
    -- orders still reference it for history — those rows just lose the table link.
    "tableId"        text REFERENCES "PosTable"(id) ON DELETE SET NULL,
    "bookingId"      text REFERENCES "Booking"(id),
    "guestName"      text,
    status           "OrderStatus" NOT NULL DEFAULT 'OPEN',
    "openedByUserId" text NOT NULL REFERENCES "User"(id),
    total            numeric(10,2) NOT NULL DEFAULT 0,
    note             text,
    "createdAt"      timestamp(3) NOT NULL DEFAULT now(),
    "updatedAt"      timestamp(3) NOT NULL DEFAULT now()
);

CREATE INDEX "Order_tableId_idx" ON "Order" ("tableId");
CREATE INDEX "Order_bookingId_idx" ON "Order" ("bookingId");
CREATE INDEX "Order_status_idx" ON "Order" ("status");

CREATE TABLE "OrderItem" (
    id           text PRIMARY KEY,
    "orderId"    text NOT NULL REFERENCES "Order"(id) ON DELETE CASCADE,
    -- No ON DELETE clause (default RESTRICT): unlike a table, a menu item that has ever been
    -- ordered can never be deleted, matching the "409 if there's an OrderItem" contract rule.
    "menuItemId" text NOT NULL REFERENCES "MenuItem"(id),
    quantity     integer NOT NULL,
    "unitPrice"  numeric(10,2) NOT NULL,
    note         text,
    "createdAt"  timestamp(3) NOT NULL DEFAULT now()
);

CREATE INDEX "OrderItem_orderId_idx" ON "OrderItem" ("orderId");
CREATE INDEX "OrderItem_menuItemId_idx" ON "OrderItem" ("menuItemId");

CREATE TABLE "Shift" (
    id                   text PRIMARY KEY,
    "openedByUserId"     text NOT NULL REFERENCES "User"(id),
    "openedAt"           timestamp(3) NOT NULL DEFAULT now(),
    "closedByUserId"     text REFERENCES "User"(id),
    "closedAt"           timestamp(3),
    "openingCashFloat"   numeric(10,2),
    "closingCashCounted" numeric(10,2),
    status               "ShiftStatus" NOT NULL DEFAULT 'OPEN',
    notes                text
);

-- One open shift per user, enforced at the DB level (not just in ShiftService) so a race
-- between two concurrent "open shift" requests from the same user can't both succeed.
CREATE UNIQUE INDEX "Shift_one_open_per_user" ON "Shift" ("openedByUserId") WHERE status = 'OPEN';

CREATE TABLE "Payment" (
    id                  text PRIMARY KEY,
    "orderId"           text NOT NULL REFERENCES "Order"(id),
    method              "PaymentMethod" NOT NULL,
    amount              numeric(10,2) NOT NULL,
    -- Set only when method = ROOM_CHARGE; see Order.bookingId comment on the folio calc.
    "bookingId"         text REFERENCES "Booking"(id),
    "recordedByUserId"  text NOT NULL REFERENCES "User"(id),
    "shiftId"           text NOT NULL REFERENCES "Shift"(id),
    "createdAt"         timestamp(3) NOT NULL DEFAULT now()
);

CREATE INDEX "Payment_shiftId_idx" ON "Payment" ("shiftId");
CREATE INDEX "Payment_bookingId_idx" ON "Payment" ("bookingId");
CREATE INDEX "Payment_orderId_idx" ON "Payment" ("orderId");
