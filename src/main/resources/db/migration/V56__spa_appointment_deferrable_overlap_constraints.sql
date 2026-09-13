-- Makes both SpaAppointment exclusion constraints DEFERRABLE INITIALLY IMMEDIATE, needed for the
-- table-swap operation (POST /spa-appointments/{id}/swap-table): swapping two appointments'
-- tables can only ever be expressed as two ordinary UPDATEs, and each one, checked as it runs
-- (the default for any NOT DEFERRABLE constraint - Postgres enforces those synchronously, per
-- row, as each row's index entry is written, not once at the end of the statement or
-- transaction), would see the other appointment still sitting on the table it's moving into and
-- fail - the exact trap two sequential single-segment calls hit on the booking side, but with no
-- single-statement escape here (a single UPDATE moving both rows was tried and still fails - see
-- SpaAppointmentTableSwapConstraintTimingTests for why, and CLAUDE.md's Concurrency section for
-- this migration's own reasoning written down for the next person who reaches for a swap and
-- assumes the constraint won't notice the detour).
--
-- INITIALLY IMMEDIATE keeps every existing write (create, updateSchedule, addTreatment) checked
-- exactly as before - deferred timing only applies once a transaction explicitly runs
-- SET CONSTRAINTS ... DEFERRED, which only SpaAppointmentService#swapTables ever does. That
-- transaction forces the check back to immediate itself (SET CONSTRAINTS ... IMMEDIATE) before
-- returning, specifically so a real conflict still surfaces as a catchable exception inside the
-- method - translated to the same friendly message every other spa write already gives - rather
-- than as an opaque failure at the outer COMMIT, after the method has already returned and any
-- audit log describing the swap may already have been written.
--
-- Same drop-then-recreate technique V43 already used to replace these two constraints' predicate
-- - no existing row can violate a constraint that's merely gained deferrability, so this is safe
-- against current data the same way V43's own comment reasons through.
ALTER TABLE "SpaAppointment" DROP CONSTRAINT spa_appointment_no_table_overlap;
ALTER TABLE "SpaAppointment" DROP CONSTRAINT spa_appointment_no_therapist_overlap;

ALTER TABLE "SpaAppointment" ADD CONSTRAINT spa_appointment_no_table_overlap
  EXCLUDE USING gist (
    "tableId" WITH =,
    tsrange(
      ("date" + "startTime"),
      ("date" + "startTime" + ("durationMinutes" * INTERVAL '1 minute'))
    ) WITH &&
  ) WHERE (status IN ('BOOKED', 'COMPLETED'))
  DEFERRABLE INITIALLY IMMEDIATE;

ALTER TABLE "SpaAppointment" ADD CONSTRAINT spa_appointment_no_therapist_overlap
  EXCLUDE USING gist (
    "therapistUserId" WITH =,
    tsrange(
      ("date" + "startTime"),
      ("date" + "startTime" + ("durationMinutes" * INTERVAL '1 minute'))
    ) WITH &&
  ) WHERE (status IN ('BOOKED', 'COMPLETED'))
  DEFERRABLE INITIALLY IMMEDIATE;
