-- The guest as a person, distinct from Booking's own frozen guestName/guestEmail/guestPhone
-- snapshot - see Guest's own description in openapi.yaml for why the two coexist and why this
-- table stores no computed/aggregated field (every fact about a guest is reached by walking to
-- Booking via guestId in V45, never cached here).
CREATE TABLE "Guest" (
    id           text PRIMARY KEY,
    name         text NOT NULL,
    email        text,
    phone        text,
    notes        text,
    "createdAt"  timestamp(3) NOT NULL DEFAULT now(),
    "updatedAt"  timestamp(3) NOT NULL DEFAULT now()
);
