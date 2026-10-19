-- Staff Telegram bot, part 1: the HR director, shift-change requests and the bot's own state.
-- The audit values these features write are in V151 (an enum value cannot be used in the transaction
-- that adds it).

-- HR is a job function, like ENGINEER / THERAPIST (V33, V37): one more value in the CHECK, no ALTER TYPE.
-- "At most one active holder" cannot be an index over a TEXT[] and is checked by UserService under a lock.
ALTER TABLE "User" DROP CONSTRAINT user_job_functions_valid;
ALTER TABLE "User" ADD CONSTRAINT user_job_functions_valid
  CHECK ("jobFunctions" <@ ARRAY['ENGINEER', 'HOUSEKEEPER', 'THERAPIST', 'HR']::text[]);

-- The employee's own choice of the evening-before reminder; null = the settings' default.
ALTER TABLE "User" ADD COLUMN "eveReminderEnabled" boolean;
-- The HR holder's leave (hotel dates, inclusive). While it lasts requests go to the administrators.
ALTER TABLE "User" ADD COLUMN "hrVacationFrom" date;
ALTER TABLE "User" ADD COLUMN "hrVacationTo" date;
ALTER TABLE "User" ADD CONSTRAINT user_hr_vacation_valid CHECK (
    ("hrVacationFrom" IS NULL) = ("hrVacationTo" IS NULL)
    AND ("hrVacationFrom" IS NULL OR "hrVacationTo" >= "hrVacationFrom"));

-- New notification types.
ALTER TABLE "Notification" DROP CONSTRAINT "Notification_type_check";
ALTER TABLE "Notification" ADD CONSTRAINT "Notification_type_check" CHECK (type IN (
    'LATE', 'ABSENT', 'BLOCK_CREATED', 'BLOCK_LAST_DAY', 'BLOCK_EXPIRED_OPEN', 'TERMINAL_OFFLINE', 'TERMINAL_ONLINE',
    'SHIFT_REQUEST_NEW', 'SHIFT_REQUEST_DECIDED', 'SHIFT_REQUEST_EXPIRED', 'SHIFT_REQUEST_REMINDER',
    'SHIFT_EVE_REMINDER', 'ROSTER_CHANGED'));

-- Which message a delivery became in the channel, so it can be edited later (Telegram: message_id).
ALTER TABLE "NotificationDelivery" ADD COLUMN "externalMessageId" text;

-- One row of bot / shift-request settings (pinned by the CHECK, seeded here, same shape as
-- NotificationSettings, V148). Kept apart from it: a different feature with its own defaults.
CREATE TABLE "StaffBotSettings" (
    id                          integer PRIMARY KEY CHECK (id = 1),
    "myShiftsEnabled"           boolean NOT NULL DEFAULT true,
    "myDayEnabled"              boolean NOT NULL DEFAULT true,
    "myMonthEnabled"            boolean NOT NULL DEFAULT true,
    "myRequestsEnabled"         boolean NOT NULL DEFAULT true,
    "botSettingsEnabled"        boolean NOT NULL DEFAULT true,
    "requestsEnabled"           boolean NOT NULL DEFAULT true,
    "moveRequestsEnabled"       boolean NOT NULL DEFAULT true,
    "deleteRequestsEnabled"     boolean NOT NULL DEFAULT true,
    "requestMinHoursBefore"     integer NOT NULL DEFAULT 24 CHECK ("requestMinHoursBefore" BETWEEN 0 AND 720),
    "allowTodayRequests"        boolean NOT NULL DEFAULT false,
    "moveWindowDays"            integer NOT NULL DEFAULT 14 CHECK ("moveWindowDays" BETWEEN 1 AND 90),
    "reasonRequired"            boolean NOT NULL DEFAULT true,
    "myShiftsDays"              integer NOT NULL DEFAULT 14 CHECK ("myShiftsDays" BETWEEN 1 AND 60),
    "fallbackToAdmin"           boolean NOT NULL DEFAULT true,
    "pendingReminderEnabled"    boolean NOT NULL DEFAULT true,
    "pendingReminderHours"      integer NOT NULL DEFAULT 12 CHECK ("pendingReminderHours" BETWEEN 1 AND 168),
    "eveReminderEnabled"        boolean NOT NULL DEFAULT true,
    "eveReminderTime"           time NOT NULL DEFAULT '18:00',
    "eveReminderDefaultOn"      boolean NOT NULL DEFAULT false,
    "rosterChangeEnabled"       boolean NOT NULL DEFAULT true,
    "rosterChangeHorizonDays"   integer NOT NULL DEFAULT 14 CHECK ("rosterChangeHorizonDays" BETWEEN 1 AND 90),
    "rosterChangeDelayMinutes"  integer NOT NULL DEFAULT 10 CHECK ("rosterChangeDelayMinutes" BETWEEN 1 AND 240),
    "updatedAt"                 timestamp(3) NOT NULL DEFAULT now()
);
INSERT INTO "StaffBotSettings" (id) VALUES (1);

-- An employee's request to move or remove one of their shifts. The snapshot columns say what the
-- entry looked like when it was filed: approving applies only if the entry still looks like that.
-- The entry is ON DELETE SET NULL, not CASCADE: the request outlives a deleted entry (and then expires).
CREATE TABLE "ShiftChangeRequest" (
    id                 text PRIMARY KEY,
    "employeeUserId"   text NOT NULL REFERENCES "User" (id),
    "rosterEntryId"    text REFERENCES "RosterEntry" (id) ON DELETE SET NULL,
    type               text NOT NULL CHECK (type IN ('MOVE', 'DELETE')),
    status             text NOT NULL DEFAULT 'PENDING' CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED', 'CANCELLED', 'EXPIRED')),
    channel            text NOT NULL CHECK (channel IN ('TELEGRAM', 'ADMIN_UI')),
    reason             text CHECK (reason IS NULL OR char_length(reason) <= 300),
    "shiftDate"        date NOT NULL,
    "shiftCodeId"      text NOT NULL REFERENCES "ShiftCode" (id),
    "shiftCode"        text NOT NULL,
    "shiftTimes"       text,
    "targetDate"       date,
    "decidedByUserId"  text REFERENCES "User" (id),
    "decidedAt"        timestamp(3),
    "decisionComment"  text CHECK ("decisionComment" IS NULL OR char_length("decisionComment") <= 300),
    "expiredReason"    text,
    "createdAt"        timestamp(3) NOT NULL DEFAULT now(),
    "updatedAt"        timestamp(3) NOT NULL DEFAULT now(),
    CONSTRAINT shift_change_request_target_matches_type CHECK ((type = 'MOVE') = ("targetDate" IS NOT NULL))
);
-- One waiting request per entry - the database's rule, not a check two taps can both pass.
CREATE UNIQUE INDEX "ShiftChangeRequest_one_pending_per_entry" ON "ShiftChangeRequest" ("rosterEntryId") WHERE status = 'PENDING';
CREATE INDEX "ShiftChangeRequest_employee_idx" ON "ShiftChangeRequest" ("employeeUserId", "createdAt");
CREATE INDEX "ShiftChangeRequest_status_idx" ON "ShiftChangeRequest" (status, "shiftDate");

-- What a button in a message does. The channel only ever sees the (short, opaque) id: callback_data
-- carries nothing else, everything is read from here. Tied to the person it is for, and to a
-- notification when it came with one (gone with it); the bot's own lists create loose ones that expire.
CREATE TABLE "StaffAction" (
    id                text PRIMARY KEY,
    "notificationId"  text REFERENCES "Notification" (id) ON DELETE CASCADE,
    "userId"          text NOT NULL REFERENCES "User" (id),
    kind              text NOT NULL,
    "subjectId"       text,
    arg               text,
    "createdAt"       timestamp(3) NOT NULL DEFAULT now(),
    "expiresAt"       timestamp(3)
);
CREATE INDEX "StaffAction_notification_idx" ON "StaffAction" ("notificationId");
CREATE INDEX "StaffAction_expires_idx" ON "StaffAction" ("expiresAt");

-- A multi-step bot dialogue (pick a date, then type the reason). Per chat, short-lived, in the database
-- so a restart does not lose it.
CREATE TABLE "BotConversation" (
    "chatId"       text PRIMARY KEY,
    "userId"       text NOT NULL REFERENCES "User" (id),
    state          text NOT NULL CHECK (state IN ('SHIFT_REASON', 'HR_VACATION_FROM', 'HR_VACATION_TO')),
    "entryId"      text,
    "requestType"  text CHECK ("requestType" IS NULL OR "requestType" IN ('MOVE', 'DELETE')),
    "targetDate"   date,
    "expiresAt"    timestamp(3) NOT NULL,
    "updatedAt"    timestamp(3) NOT NULL DEFAULT now()
);

-- "The roster changed" is announced once per employee and day, after the changes stop: each manager
-- gesture only touches the mark; a sweep announces it. beforeShiftCodeId is the entry as it was when the
-- first change of the burst happened (null = the day was off).
CREATE TABLE "RosterChangeMark" (
    id                  text PRIMARY KEY,
    -- CASCADE: a mark is a pending announcement, nothing worth keeping once its person is gone.
    "employeeUserId"    text NOT NULL REFERENCES "User" (id) ON DELETE CASCADE,
    "shiftDate"         date NOT NULL,
    "beforeShiftCodeId" text,
    "firstChangedAt"    timestamp(3) NOT NULL,
    "lastChangedAt"     timestamp(3) NOT NULL
);
CREATE UNIQUE INDEX "RosterChangeMark_employee_date_key" ON "RosterChangeMark" ("employeeUserId", "shiftDate");
CREATE INDEX "RosterChangeMark_last_idx" ON "RosterChangeMark" ("lastChangedAt");
