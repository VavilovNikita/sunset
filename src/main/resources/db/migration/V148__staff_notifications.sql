-- Staff notification centre. The core (Notification / NotificationDelivery) knows nothing about
-- Telegram: a channel is a row value ('TELEGRAM' today; LINE / WhatsApp later widen the CHECKs).
-- Sets of values are TEXT + CHECK, not native enums, for the same reason as JobFunction (V33).
-- The audit values these features write are in V149 (an enum value cannot be used in the
-- transaction that adds it).

-- One row of thresholds, quiet hours and recipients (pinned by the CHECK, seeded here, same shape
-- as LifecycleEmailSettings, V112).
CREATE TABLE "NotificationSettings" (
    id                        integer PRIMARY KEY CHECK (id = 1),
    "lateEnabled"             boolean NOT NULL DEFAULT true,
    "lateGraceMinutes"        integer NOT NULL DEFAULT 10 CHECK ("lateGraceMinutes" BETWEEN 1 AND 240),
    "absenceEnabled"          boolean NOT NULL DEFAULT true,
    "absenceMinutes"          integer NOT NULL DEFAULT 60 CHECK ("absenceMinutes" BETWEEN 1 AND 1440),
    "blockCreatedEnabled"     boolean NOT NULL DEFAULT true,
    "blockLastDayEnabled"     boolean NOT NULL DEFAULT true,
    "blockExpiredEnabled"     boolean NOT NULL DEFAULT true,
    "terminalAlertsEnabled"   boolean NOT NULL DEFAULT true,
    "terminalSilenceMinutes"  integer NOT NULL DEFAULT 15 CHECK ("terminalSilenceMinutes" BETWEEN 1 AND 1440),
    "quietHoursEnabled"       boolean NOT NULL DEFAULT true,
    "quietStart"              time NOT NULL DEFAULT '22:00',
    "quietEnd"                time NOT NULL DEFAULT '07:00',
    "morningTime"             time NOT NULL DEFAULT '09:00',
    "defaultLanguage"         text NOT NULL DEFAULT 'EN' CHECK ("defaultLanguage" IN ('EN', 'TH', 'RU')),
    -- User ids. No foreign key on purpose (an array element cannot have one); a user who is gone or
    -- deactivated is simply skipped when recipients are resolved.
    "lateRecipientUserIds"    text[] NOT NULL DEFAULT '{}',
    "directorUserIds"         text[] NOT NULL DEFAULT '{}',
    "updatedAt"               timestamp(3) NOT NULL DEFAULT now()
);
INSERT INTO "NotificationSettings" (id) VALUES (1);

-- A person's own language; null = the settings' default.
ALTER TABLE "User" ADD COLUMN "notificationLanguage" text;
ALTER TABLE "User" ADD CONSTRAINT user_notification_language_valid
    CHECK ("notificationLanguage" IS NULL OR "notificationLanguage" IN ('EN', 'TH', 'RU'));

-- Who answers for a block: ENGINEER or HOUSEKEEPER. Null on blocks created before this existed.
ALTER TABLE "RoomUnitBlock" ADD COLUMN "responsibleFunction" text;
ALTER TABLE "RoomUnitBlock" ADD CONSTRAINT room_unit_block_responsible_function_valid
    CHECK ("responsibleFunction" IS NULL OR "responsibleFunction" IN ('ENGINEER', 'HOUSEKEEPER'));

-- One message to one person. "text" is the message already rendered in the recipient's language at
-- the moment it was raised, so a later language or wording change never rewrites history and a
-- retry sends exactly what the log shows. The unique index is what makes a repeated scanner pass
-- (or two passes racing) raise nothing twice.
CREATE TABLE "Notification" (
    id                text PRIMARY KEY,
    type              text NOT NULL CHECK (type IN (
        'LATE', 'ABSENT', 'BLOCK_CREATED', 'BLOCK_LAST_DAY', 'BLOCK_EXPIRED_OPEN', 'TERMINAL_OFFLINE', 'TERMINAL_ONLINE')),
    "messageKey"      text NOT NULL,
    "recipientUserId" text NOT NULL REFERENCES "User" (id),
    language          text NOT NULL CHECK (language IN ('EN', 'TH', 'RU')),
    text              text NOT NULL,
    "dedupKey"        text NOT NULL,
    "createdAt"       timestamp(3) NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX "Notification_dedup_recipient_key" ON "Notification" ("dedupKey", "recipientUserId");
CREATE INDEX "Notification_createdAt_idx" ON "Notification" ("createdAt");

-- Delivery of one notification over one channel. "nextAttemptAt" is both the retry time and the
-- end of quiet hours for a message raised during them.
CREATE TABLE "NotificationDelivery" (
    id                text PRIMARY KEY,
    "notificationId"  text NOT NULL REFERENCES "Notification" (id) ON DELETE CASCADE,
    channel           text NOT NULL CHECK (channel IN ('TELEGRAM')),
    status            text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'SENT', 'FAILED')),
    attempts          integer NOT NULL DEFAULT 0,
    -- Our own words, never the channel's raw answer.
    "lastError"       text,
    "nextAttemptAt"   timestamp(3) NOT NULL,
    "sentAt"          timestamp(3),
    "createdAt"       timestamp(3) NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX "NotificationDelivery_notification_channel_key" ON "NotificationDelivery" ("notificationId", channel);
CREATE INDEX "NotificationDelivery_due_idx" ON "NotificationDelivery" (status, "nextAttemptAt");

-- A person's account on a channel. "externalId" is Telegram's chat id.
CREATE TABLE "StaffChannelLink" (
    id                 text PRIMARY KEY,
    "userId"           text NOT NULL REFERENCES "User" (id),
    channel            text NOT NULL CHECK (channel IN ('TELEGRAM')),
    "externalId"       text NOT NULL,
    "createdAt"        timestamp(3) NOT NULL DEFAULT now()
);
-- One chat belongs to at most one employee, and an employee has at most one chat per channel.
CREATE UNIQUE INDEX "StaffChannelLink_channel_external_key" ON "StaffChannelLink" (channel, "externalId");
CREATE UNIQUE INDEX "StaffChannelLink_user_channel_key" ON "StaffChannelLink" ("userId", channel);

-- The single-use code behind a t.me/<bot>?start=<code> link. The code itself is the key.
CREATE TABLE "ChannelLinkCode" (
    code               text PRIMARY KEY CHECK (code ~ '^[A-Za-z0-9_-]{1,64}$'),
    "userId"           text NOT NULL REFERENCES "User" (id),
    channel            text NOT NULL CHECK (channel IN ('TELEGRAM')),
    "createdByUserId"  text,
    "createdAt"        timestamp(3) NOT NULL DEFAULT now(),
    "expiresAt"        timestamp(3) NOT NULL,
    "usedAt"           timestamp(3)
);
CREATE INDEX "ChannelLinkCode_user_idx" ON "ChannelLinkCode" ("userId");

-- Telegram polling state: the getUpdates offset lives in the database so a restart neither replays
-- nor loses updates; the bot's username (from getMe) is cached so no request has to ask Telegram.
CREATE TABLE "TelegramBotState" (
    id               integer PRIMARY KEY CHECK (id = 1),
    "updateOffset"   bigint NOT NULL DEFAULT 0,
    "botUsername"    text,
    "updatedAt"      timestamp(3) NOT NULL DEFAULT now()
);
INSERT INTO "TelegramBotState" (id) VALUES (1);

-- One row per attendance terminal currently silent: what makes "terminal not responding" one
-- message per episode. "anchorLastSeenAt" is the terminal's lastSeenAt when the silence began.
CREATE TABLE "TerminalSilenceEpisode" (
    "deviceId"          text PRIMARY KEY,
    "anchorLastSeenAt"  timestamp(3),
    "startedAt"         timestamp(3) NOT NULL DEFAULT now()
);
