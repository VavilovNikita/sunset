package com.sunsetbeach.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.OffsetDateTime;
import org.junit.jupiter.api.Test;

/**
 * Regression guard, not a static-code check: {@code guestEmail}/{@code guestPhone}/
 * {@code paymentNote} were hand-redacted out of these four generated models' {@code toString()}
 * (see EmailService's javadoc and the security-audit fix for why - PII must never leak into a
 * stray {@code log.debug("{}", booking)}), but a hand-edit to codegen'd source has no mechanism
 * keeping it in place: the next full regeneration from openapi.yaml silently restores the
 * default {@code toString()}, which prints every field. A code review of the generator templates
 * wouldn't catch that; only a test that actually builds one of these objects with real-looking
 * PII and greps its own {@code toString()} output would. If this test ever starts failing, the
 * redaction was lost to a regeneration and needs to be re-applied to the model file(s) named in
 * the failure before anything else ships.
 */
class ToStringRedactsGuestPiiTests {

    private static final String REAL_EMAIL = "jane.doe@example.com";
    private static final String REAL_PHONE = "+66891234567";
    private static final String REAL_PAYMENT_NOTE = "Card ending 4242, ref JD-2031";

    @Test
    void booking_toStringDoesNotContainGuestEmailPhoneOrPaymentNote() {
        Booking booking = new Booking(
                "booking-1",
                "room-1",
                null,
                null,
                null,
                "Jane Doe",
                REAL_EMAIL,
                REAL_PHONE,
                "2026-01-01",
                "2026-01-02",
                "1500.00",
                BookingStatus.NEW,
                REAL_PAYMENT_NOTE,
                java.util.List.of(),
                OffsetDateTime.now(),
                OffsetDateTime.now());

        String rendered = booking.toString();

        assertThat(rendered).doesNotContain(REAL_EMAIL).doesNotContain(REAL_PHONE).doesNotContain(REAL_PAYMENT_NOTE);
    }

    @Test
    void bookingCreateInput_toStringDoesNotContainGuestEmailOrPhone() {
        BookingCreateInput input =
                new BookingCreateInput("room-1", "Jane Doe", REAL_EMAIL, REAL_PHONE, "2031-01-01", "2031-01-02");

        String rendered = input.toString();

        assertThat(rendered).doesNotContain(REAL_EMAIL).doesNotContain(REAL_PHONE);
    }

    @Test
    void staffBookingCreateInput_toStringDoesNotContainGuestEmailOrPhone() {
        StaffBookingCreateInput input = new StaffBookingCreateInput("room-1", "Jane Doe", "2031-01-01", "2031-01-02")
                .guestEmail(REAL_EMAIL)
                .guestPhone(REAL_PHONE);

        String rendered = input.toString();

        assertThat(rendered).doesNotContain(REAL_EMAIL).doesNotContain(REAL_PHONE);
    }

    @Test
    void bookingStatusInput_toStringDoesNotContainPaymentNote() {
        BookingStatusInput input = new BookingStatusInput(BookingStatus.CANCELLED).paymentNote(REAL_PAYMENT_NOTE);

        String rendered = input.toString();

        assertThat(rendered).doesNotContain(REAL_PAYMENT_NOTE);
    }
}
