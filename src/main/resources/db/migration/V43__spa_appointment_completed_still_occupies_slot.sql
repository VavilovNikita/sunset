-- Correction: COMPLETED must keep holding its slot. V41 restricted both exclusion constraints to
-- WHERE (status = 'BOOKED'), meant to stop CANCELLED/NO_SHOW from holding a slot forever - but
-- that predicate also released a COMPLETED appointment's slot the instant it was marked done.
-- Completing a 10:00-11:00 treatment at 10:20 must not free that table/therapist for a new
-- booking while the treatment is still physically running. Only CANCELLED and NO_SHOW genuinely
-- give the slot back; COMPLETED and BOOKED both still occupy it.
--
-- Replacing an already-existing constraint: drop, then re-add under the same name with the wider
-- predicate (same technique as any ALTER TABLE ... DROP/ADD CONSTRAINT). No existing SpaAppointment
-- row can violate the new predicate today - this table has no production data yet, the module
-- hasn't shipped. On a system that already had COMPLETED rows, re-adding these constraints could
-- fail right here if any two rows on the same table/therapist (COMPLETED-COMPLETED or
-- COMPLETED-BOOKED) already overlap - Flyway would halt with Postgres's own exclusion-violation
-- error naming the offending rows, and that data would need manual resolution before this
-- migration could apply. It doesn't need CREATE EXTENSION again - btree_gist was installed by V41
-- and extensions aren't dropped by dropping a constraint that merely used it.
ALTER TABLE "SpaAppointment" DROP CONSTRAINT spa_appointment_no_table_overlap;
ALTER TABLE "SpaAppointment" DROP CONSTRAINT spa_appointment_no_therapist_overlap;

ALTER TABLE "SpaAppointment" ADD CONSTRAINT spa_appointment_no_table_overlap
  EXCLUDE USING gist (
    "tableId" WITH =,
    tsrange(
      ("date" + "startTime"),
      ("date" + "startTime" + ("durationMinutes" * INTERVAL '1 minute'))
    ) WITH &&
  ) WHERE (status IN ('BOOKED', 'COMPLETED'));

ALTER TABLE "SpaAppointment" ADD CONSTRAINT spa_appointment_no_therapist_overlap
  EXCLUDE USING gist (
    "therapistUserId" WITH =,
    tsrange(
      ("date" + "startTime"),
      ("date" + "startTime" + ("durationMinutes" * INTERVAL '1 minute'))
    ) WITH &&
  ) WHERE (status IN ('BOOKED', 'COMPLETED'));
