-- Makes an otherwise-invisible degradation visible: a device whose firmware rejects the ranged
-- read (CMD_ATTLOG_TIME_RRQ) falls back to a full log read every poll, forever, with nothing
-- anywhere saying so - see ZkTerminalClientImpl#readWindowedAttendanceLog and
-- TerminalPollResult#windowedReadUnsupported's own comments. Set from what the device actually
-- answered on the most recent poll that attempted a windowed read, never from a configuration
-- guess, and cleared the moment a later poll succeeds with a window (firmware can be updated, or
-- a replacement unit swapped in under the same row).
ALTER TABLE "AttendanceDevice" ADD COLUMN "windowedReadUnsupported" boolean NOT NULL DEFAULT false;
