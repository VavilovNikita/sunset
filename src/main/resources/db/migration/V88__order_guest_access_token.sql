-- Backs the dine-in QR ordering feature: a per-order random token a guest's phone presents
-- instead of logging in (see Order.guestAccessToken's own openapi.yaml description and
-- OrderService#requireGuestAccess). Nullable and not backfilled - existing orders simply have no
-- token and are never guest-reachable, which is fine, since they're never "the order for a table
-- sitting right now" by the time this migration runs. Unique so two orders can never collide on
-- the same token (OrderService#create only ever writes a freshly generated 24-random-byte value,
-- but the index is what actually rules out a collision, not the odds alone).
ALTER TABLE "Order" ADD COLUMN "guestAccessToken" text;
CREATE UNIQUE INDEX "Order_guestAccessToken_key" ON "Order" ("guestAccessToken");
