-- What the persistent menu keyboard under a chat's input field looks like right now (the language and the labels
-- it was sent with). A keyboard can only be replaced by sending a message that carries one, so the bot compares
-- this with what the person's menu should be at its next reply and re-sends the menu only when they differ
-- (a manager changed their language, a screen was switched off). NULL = never recorded: the next reply sends it.
ALTER TABLE "StaffChannelLink" ADD COLUMN "menuSignature" text;
