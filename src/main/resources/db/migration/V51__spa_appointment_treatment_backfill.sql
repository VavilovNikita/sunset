-- One child row per existing SpaAppointment, copying its current treatmentMenuItemId/
-- durationMinutes exactly. SpaAppointment.durationMinutes is already correct for a
-- single-element sum, so the parent row needs no recomputation - the maintained-sum invariant
-- holds from this migration onward with no further write.
INSERT INTO "SpaAppointmentTreatment" (id, "spaAppointmentId", "treatmentMenuItemId", "durationMinutes", "createdAt")
SELECT gen_random_uuid()::text, id, "treatmentMenuItemId", "durationMinutes", "createdAt"
FROM "SpaAppointment";
