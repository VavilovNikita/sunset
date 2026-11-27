-- The directors are told about a new note in the staff manual's "What's new" (CLAUDE.md, Staff notifications):
-- a switch in the alert settings, a new notification type, and what the server remembers about each note it has seen.

ALTER TABLE "NotificationSettings" ADD COLUMN "manualUpdatesEnabled" boolean NOT NULL DEFAULT true;

ALTER TABLE "Notification" DROP CONSTRAINT "Notification_type_check";
ALTER TABLE "Notification" ADD CONSTRAINT "Notification_type_check" CHECK (type IN (
    'LATE', 'ABSENT', 'BLOCK_CREATED', 'BLOCK_LAST_DAY', 'BLOCK_EXPIRED_OPEN', 'TERMINAL_OFFLINE', 'TERMINAL_ONLINE',
    'SHIFT_REQUEST_NEW', 'SHIFT_REQUEST_DECIDED', 'SHIFT_REQUEST_EXPIRED', 'SHIFT_REQUEST_REMINDER',
    'SHIFT_EVE_REMINDER', 'ROSTER_CHANGED', 'MORNING_SUMMARY', 'THERAPIST_MORNING', 'MANUAL_UPDATED'));

-- One row per note of the feed that needs no further look: SENT (told, on the hotel day the message is due),
-- HELD (that day already had its message: it goes with the next one) or SKIPPED (seen while switched off: never sent).
-- "entryId" is the note's id in the manual's updates.json (its file name, e.g. 2026-11-28).
CREATE TABLE "ManualUpdateEntry" (
    "entryId"   text PRIMARY KEY,
    state       text NOT NULL CHECK (state IN ('SENT', 'HELD', 'SKIPPED')),
    "sentOn"    date,
    "createdAt" timestamp(3) NOT NULL,
    CONSTRAINT manual_update_entry_sent_has_a_day CHECK ((state = 'SENT') = ("sentOn" IS NOT NULL))
);
CREATE INDEX "ManualUpdateEntry_sentOn_idx" ON "ManualUpdateEntry" ("sentOn");
