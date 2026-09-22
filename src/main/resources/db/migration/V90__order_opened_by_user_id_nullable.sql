-- Room-service ordering (GuestOrder) opens an Order on a guest's own behalf - there's no staff
-- User row to point at, so "openedByUserId" (originally NOT NULL, see V3) must accept null for
-- exactly that case. See Order.openedByUserId's own openapi.yaml description and
-- OrderService#resolveEmail for how a null id is turned into a human-readable label instead of
-- being looked up.
ALTER TABLE "Order" ALTER COLUMN "openedByUserId" DROP NOT NULL;
