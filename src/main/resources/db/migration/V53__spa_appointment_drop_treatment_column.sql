-- The singular treatmentMenuItemId superseded by SpaAppointmentTreatment (V50/V51) is now fully
-- unread - a full sweep (backend and frontend, see CLAUDE.md's Spa billing section) found no
-- remaining reader once V50/V51/V52's own change landed: the entity field/getter/setter are
-- already gone, and the two test files that used to set it directly (bypassing
-- SpaAppointmentService, for order-link tests that never touch a treatment's own content) no
-- longer do. Separate migration, separate commit from the table/backfill, so this specific step
-- can be reverted on its own if the sweep missed something.
ALTER TABLE "SpaAppointment" DROP COLUMN "treatmentMenuItemId";
