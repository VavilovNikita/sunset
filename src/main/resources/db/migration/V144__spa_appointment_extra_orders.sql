-- A spa appointment can be billed by more than one order. SpaAppointment.orderId stays "the
-- appointment's own order" (the first one, set by the spa billing door or the auto-link); this table
-- holds the further ones - today the top-up order opened when the first order is already PAID and a
-- treatment is still unbilled (POST /spa-appointments/{id}/bill-missing-treatments). Completeness of
-- payment and the "not billed" warning are computed over the appointment's order plus these.
--
-- An order belongs to at most one appointment (orderId is the primary key). Both foreign keys
-- cascade: a link row is only a pointer and must never stand in the way of removing the appointment
-- or the order (test clean-up deletes both).
CREATE TABLE "SpaAppointmentOrder" (
    "orderId"          text PRIMARY KEY REFERENCES "Order"(id) ON DELETE CASCADE,
    "spaAppointmentId" text NOT NULL REFERENCES "SpaAppointment"(id) ON DELETE CASCADE,
    "createdAt"        timestamp(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX "SpaAppointmentOrder_spaAppointmentId_idx" ON "SpaAppointmentOrder" ("spaAppointmentId");
