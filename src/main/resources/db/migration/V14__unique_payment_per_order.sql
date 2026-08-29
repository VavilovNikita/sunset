-- An order has no partial-payment model: OrderService.close() posts exactly one Payment and
-- moves the order straight to PAID. The read-then-write status check in that method is not
-- enough on its own to stop two concurrent POST /orders/{id}/close calls (a double-tap at the
-- terminal, or a client retry after a network timeout) from both reading OPEN before either
-- commits and both posting a Payment - which would double-charge the guest's room folio for a
-- ROOM_CHARGE close. A unique constraint makes the second insert fail at the database instead of
-- silently succeeding; OrderService now maps that failure to a 409 Conflict.
ALTER TABLE "Payment" ADD CONSTRAINT "Payment_orderId_key" UNIQUE ("orderId");
