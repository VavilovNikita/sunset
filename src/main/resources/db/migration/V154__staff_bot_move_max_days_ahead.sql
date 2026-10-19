-- A shift move had one limit, moveWindowDays (14), both "how many days the bot shows as buttons" and "how far
-- ahead a move may go". Now the window only says how many nearest days are buttons; moveMaxDaysAhead is how far a
-- move may go at all (the "Other date..." calendar and a typed date). Never less than the window.
ALTER TABLE "StaffBotSettings"
    ADD COLUMN "moveMaxDaysAhead" integer NOT NULL DEFAULT 60;
-- A window set above the default (up to 90) must not leave the row violating the new rule.
UPDATE "StaffBotSettings" SET "moveMaxDaysAhead" = GREATEST("moveMaxDaysAhead", "moveWindowDays");
ALTER TABLE "StaffBotSettings"
    ADD CONSTRAINT "StaffBotSettings_moveMaxDaysAhead_range" CHECK ("moveMaxDaysAhead" BETWEEN 1 AND 180 AND "moveMaxDaysAhead" >= "moveWindowDays");

-- The calendar of "Other date..." waits for a typed date: a new dialogue state next to the existing three.
ALTER TABLE "BotConversation" DROP CONSTRAINT "BotConversation_state_check";
ALTER TABLE "BotConversation"
    ADD CONSTRAINT "BotConversation_state_check" CHECK (state IN ('SHIFT_REASON', 'SHIFT_DATE', 'HR_VACATION_FROM', 'HR_VACATION_TO'));
