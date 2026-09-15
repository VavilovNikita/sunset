-- A fingerprint terminal the server polls over TCP/IP (ZKTeco K60, port 4370) - the server reads
-- the device's own attendance log, the device never calls us. See AttendanceDevice's own
-- openapi.yaml description for why that direction is deliberate and what it changes about this
-- deployment's security posture.
CREATE TABLE "AttendanceDevice" (
    id           text PRIMARY KEY,
    name         text NOT NULL,
    serial       text NOT NULL,
    address      text NOT NULL,
    port         integer NOT NULL DEFAULT 4370,
    -- IANA zone id (e.g. "Asia/Bangkok") - the device's own clock, not this server's. The polling
    -- service sets the device's clock to ours on every successful poll (see that service's own
    -- comment for why), but a punch read before that write lands is timestamped in whatever zone
    -- the device itself was already in - this column is what makes that reading correct instead
    -- of a guess.
    timezone     text NOT NULL,
    active       boolean NOT NULL DEFAULT true,
    "lastSeenAt" timestamp(3),
    "createdAt"  timestamp(3) NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX "AttendanceDevice_serial_key" ON "AttendanceDevice" (serial);
