-- Staff Telegram bot, part 2: role screens (engineer's tasks, housekeeper's blocks, therapist's procedures,
-- "who is on shift", the managers' summary) and the morning messages. Every screen and every automatic message
-- has its own switch here, and every horizon and time is a column - none is a constant in code.
-- The audit value this writes is in V153 (an enum value cannot be used in the transaction that adds it).

ALTER TABLE "StaffBotSettings"
    ADD COLUMN "engineerTasksEnabled"       boolean NOT NULL DEFAULT true,
    ADD COLUMN "housekeeperBlocksEnabled"   boolean NOT NULL DEFAULT true,
    ADD COLUMN "housekeeperBlocksDays"      integer NOT NULL DEFAULT 7 CHECK ("housekeeperBlocksDays" BETWEEN 1 AND 60),
    ADD COLUMN "therapistScheduleEnabled"   boolean NOT NULL DEFAULT true,
    ADD COLUMN "therapistScheduleDays"      integer NOT NULL DEFAULT 2 CHECK ("therapistScheduleDays" BETWEEN 1 AND 14),
    ADD COLUMN "therapistMorningEnabled"    boolean NOT NULL DEFAULT true,
    ADD COLUMN "therapistMorningTime"       time NOT NULL DEFAULT '08:00',
    ADD COLUMN "therapistMorningDefaultOn"  boolean NOT NULL DEFAULT false,
    ADD COLUMN "whoIsOnShiftEnabled"        boolean NOT NULL DEFAULT true,
    ADD COLUMN "whoIsOnShiftForHr"          boolean NOT NULL DEFAULT true,
    ADD COLUMN "summaryCommandEnabled"      boolean NOT NULL DEFAULT true,
    ADD COLUMN "morningSummaryEnabled"      boolean NOT NULL DEFAULT true,
    ADD COLUMN "morningSummaryTime"         time NOT NULL DEFAULT '08:00',
    ADD COLUMN "summaryArrivals"            boolean NOT NULL DEFAULT true,
    ADD COLUMN "summaryDepartures"          boolean NOT NULL DEFAULT true,
    ADD COLUMN "summaryInHouse"             boolean NOT NULL DEFAULT true,
    ADD COLUMN "summaryOccupancy"           boolean NOT NULL DEFAULT true,
    ADD COLUMN "summaryRooms"               boolean NOT NULL DEFAULT true,
    ADD COLUMN "summaryStaff"               boolean NOT NULL DEFAULT true,
    ADD COLUMN "summaryRequests"            boolean NOT NULL DEFAULT true;

-- A therapist's own choice of the morning message; null = the settings' default (off).
ALTER TABLE "User" ADD COLUMN "therapistMorningEnabled" boolean;

ALTER TABLE "Notification" DROP CONSTRAINT "Notification_type_check";
ALTER TABLE "Notification" ADD CONSTRAINT "Notification_type_check" CHECK (type IN (
    'LATE', 'ABSENT', 'BLOCK_CREATED', 'BLOCK_LAST_DAY', 'BLOCK_EXPIRED_OPEN', 'TERMINAL_OFFLINE', 'TERMINAL_ONLINE',
    'SHIFT_REQUEST_NEW', 'SHIFT_REQUEST_DECIDED', 'SHIFT_REQUEST_EXPIRED', 'SHIFT_REQUEST_REMINDER',
    'SHIFT_EVE_REMINDER', 'ROSTER_CHANGED', 'MORNING_SUMMARY', 'THERAPIST_MORNING'));
