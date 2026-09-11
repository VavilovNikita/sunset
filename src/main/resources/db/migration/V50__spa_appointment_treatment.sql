-- A child row per treatment on a SpaAppointment, replacing the old singular
-- SpaAppointment.treatmentMenuItemId - same treatment added twice is two rows (no quantity
-- field), so a doubled booking is expressed in the data, not inferred from a count. The old
-- column stays on SpaAppointment, unused by new code, until V53 drops it after a full reader
-- sweep - see CLAUDE.md's Spa billing section for why the drop waits.
--
-- durationMinutes is copied from MenuItem.durationMinutes once, at add time - the same "agreed
-- terms are frozen" precedent the old singular column already used. Deliberately no price
-- column here: unlike a room night, a spa treatment bills through the ordinary POS OrderItem/
-- menu path, which has exactly one pricing rule project-wide (read the menu live, at the moment
-- a line is added to an order) - see CLAUDE.md's Spa billing section for why freezing a second
-- price here would just manufacture a number that can silently disagree with the one actually
-- charged, instead of protecting a guest-facing quote (none is shown at booking time).
CREATE TABLE "SpaAppointmentTreatment" (
    id                     text PRIMARY KEY,
    "spaAppointmentId"     text NOT NULL REFERENCES "SpaAppointment"(id),
    "treatmentMenuItemId"  text NOT NULL REFERENCES "MenuItem"(id),
    "durationMinutes"      integer NOT NULL CHECK ("durationMinutes" > 0),
    "createdAt"            timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- The day grid's batched join and the addTreatment/removeTreatment read path both key off this.
CREATE INDEX "SpaAppointmentTreatment_spaAppointmentId_idx" ON "SpaAppointmentTreatment" ("spaAppointmentId");

-- SpaAppointmentService stops writing SpaAppointment.treatmentMenuItemId as of this change (every
-- treatment, including a newly-created appointment's first, now lives only in the table above) -
-- its NOT NULL from V41 would otherwise reject every new appointment. The column itself stays
-- (still populated on every row created before this migration) until V53 drops it outright, once
-- a full reader sweep confirms nothing still depends on it - see CLAUDE.md's Spa billing section.
ALTER TABLE "SpaAppointment" ALTER COLUMN "treatmentMenuItemId" DROP NOT NULL;
