-- WLED lighting controllers on the hotel LAN, controlled from the admin site. Which controllers exist
-- is runtime data an ADMIN enters (host/port), so nothing is seeded here. The audit values this
-- feature writes are in V147 (an enum value cannot be used in the transaction that adds it).
CREATE TABLE "LightingDevice" (
    id          text PRIMARY KEY,
    name        text NOT NULL,
    -- IP address or hostname; the server checks on save AND on every request that it resolves to a
    -- private LAN address (SSRF guard, see CLAUDE.md "Lighting (WLED)").
    host        text NOT NULL,
    port        integer NOT NULL DEFAULT 80 CHECK (port BETWEEN 1 AND 65535),
    location    text,
    active      boolean NOT NULL DEFAULT true,
    "sortOrder" integer NOT NULL DEFAULT 0,
    "createdAt" timestamp(3) NOT NULL DEFAULT now()
);

-- Unique ignoring case, outer spaces and the width of inner runs of spaces - same rule and same
-- function as rooms and physical rooms (V143).
CREATE UNIQUE INDEX "LightingDevice_name_normalized_key" ON "LightingDevice" (sunset_normalize_name(name));
