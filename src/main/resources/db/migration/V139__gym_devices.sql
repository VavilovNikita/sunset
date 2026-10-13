-- Gym equipment controlled through the Tuya cloud API. Which devices exist is runtime data an ADMIN
-- picks from the Tuya project's own list (GET /gym/settings/tuya/devices), so nothing is seeded here.
CREATE TABLE "GymDevice" (
    id              text PRIMARY KEY,
    name            text NOT NULL,
    -- RELAY: a Wi-Fi switch, real state readable. AC_IR: an air conditioner under an IR hub, one-way.
    -- A CHECK, not a native enum: same trade-off as User.jobFunctions (see V33).
    type            text NOT NULL CHECK (type IN ('RELAY', 'AC_IR')),
    "tuyaDeviceId"  text NOT NULL,
    "infraredId"    text,
    "remoteId"      text,
    "switchCode"    text,
    "sortOrder"     integer NOT NULL DEFAULT 0,
    active          boolean NOT NULL DEFAULT true,
    -- The last command we sent to an air conditioner (the IR link cannot report real state).
    -- lastAcSentAt is UTC wall-clock like every timestamp column; lastAcSentBy is a name snapshot.
    "lastAcPower"   integer,
    "lastAcMode"    integer,
    "lastAcTemp"    integer,
    "lastAcWind"    integer,
    "lastAcSentAt"  timestamp(3),
    "lastAcSentBy"  text,
    "createdAt"     timestamp(3) NOT NULL DEFAULT now()
);

CREATE UNIQUE INDEX "GymDevice_tuyaDeviceId_key" ON "GymDevice" ("tuyaDeviceId");

-- Calls made to Tuya per calendar month in hotel (Asia/Bangkok) time, key 'YYYY-MM'. The Trial
-- Edition stops at its monthly quota; this is how an ADMIN sees how much is left.
CREATE TABLE "TuyaApiUsage" (
    month text PRIMARY KEY,
    calls bigint NOT NULL DEFAULT 0
);
