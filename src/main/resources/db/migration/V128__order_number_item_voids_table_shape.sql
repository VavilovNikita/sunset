-- Three POS additions, none of them touching existing rows' meaning.
--
-- 1. Order.number: a human-readable, monotonically increasing receipt number shown and printed
--    next to the internal id (which stays the key). Existing orders are numbered in the order
--    they were opened, so the history reads 1, 2, 3... from the first order this system took; new
--    orders take the next value from the sequence at insert time. One global sequence, not per
--    day: a number never repeats, so "#1234" on a receipt identifies exactly one order forever.
ALTER TABLE "Order" ADD COLUMN "number" bigint;

UPDATE "Order" o
SET "number" = numbered.rn
FROM (SELECT id, row_number() OVER (ORDER BY "createdAt", id) AS rn FROM "Order") numbered
WHERE o.id = numbered.id;

CREATE SEQUENCE "Order_number_seq" OWNED BY "Order"."number";
SELECT setval('"Order_number_seq"', COALESCE((SELECT max("number") FROM "Order"), 0) + 1, false);
ALTER TABLE "Order" ALTER COLUMN "number" SET DEFAULT nextval('"Order_number_seq"');
ALTER TABLE "Order" ALTER COLUMN "number" SET NOT NULL;
CREATE UNIQUE INDEX "Order_number_key" ON "Order" ("number");

-- 2. OrderItemVoid: what was taken off an order after it had already gone to the kitchen/bar.
--    The voided quantity leaves OrderItem (so every reader that sums or counts lines - the total,
--    the ledger posting at close, Z410, the spa "missing treatment" check, the receipt - stays
--    correct without knowing voids exist), and is recorded here with who, when and why. Rows are
--    never updated or deleted by the application.
CREATE TABLE "OrderItemVoid" (
    id                text PRIMARY KEY,
    "orderId"         text NOT NULL REFERENCES "Order"(id) ON DELETE CASCADE,
    "menuItemId"      text NOT NULL REFERENCES "MenuItem"(id),
    quantity          integer NOT NULL CHECK (quantity > 0),
    "unitPrice"       numeric(10,2) NOT NULL,
    note              text,
    "sentAt"          timestamp(3),
    reason            text NOT NULL CHECK (length(btrim(reason)) > 0),
    "voidedByUserId"  text NOT NULL REFERENCES "User"(id),
    "voidedAt"        timestamp(3) NOT NULL DEFAULT now()
);

CREATE INDEX "OrderItemVoid_orderId_idx" ON "OrderItemVoid" ("orderId");

-- 3. PosTable.shape: how a table is drawn on the floor plan. ROUND is the default because every
--    tile was drawn round before this column existed.
CREATE TYPE "TableShape" AS ENUM ('ROUND', 'SQUARE', 'RECTANGLE');
ALTER TABLE "PosTable" ADD COLUMN shape "TableShape" NOT NULL DEFAULT 'ROUND';
