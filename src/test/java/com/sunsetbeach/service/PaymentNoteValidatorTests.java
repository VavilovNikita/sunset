package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Pure unit tests (no Spring context) for the Luhn-based heuristic that flags a card number in
 * {@code Booking.paymentNote}. Test numbers below are the standard publicly-documented test
 * card numbers (Visa/Mastercard test ranges) - they are not real, active cards.
 */
class PaymentNoteValidatorTests {

    @Test
    void plainVisaTestNumber_isDetected() {
        assertThat(PaymentNoteValidator.looksLikeCardNumber("4111111111111111")).isTrue();
    }

    @Test
    void mastercardTestNumber_isDetected() {
        assertThat(PaymentNoteValidator.looksLikeCardNumber("5500005555555559")).isTrue();
    }

    @Test
    void cardNumberWithSpaces_isDetected() {
        assertThat(PaymentNoteValidator.looksLikeCardNumber("Paid by card 4111 1111 1111 1111, thanks")).isTrue();
    }

    @Test
    void cardNumberWithDashes_isDetected() {
        assertThat(PaymentNoteValidator.looksLikeCardNumber("4111-1111-1111-1111")).isTrue();
    }

    @Test
    void ordinaryText_isNotFlagged() {
        assertThat(PaymentNoteValidator.looksLikeCardNumber("Paid via bank transfer, ref #4521")).isFalse();
    }

    @Test
    void nonLuhnDigitRun_isNotFlagged() {
        // 13 digits, deliberately fails Luhn - the kind of arbitrary booking/receipt reference
        // a plain digit-count check would incorrectly reject.
        assertThat(PaymentNoteValidator.looksLikeCardNumber("Receipt 1234567890123")).isFalse();
    }

    @Test
    void nullNote_isNotFlagged() {
        assertThat(PaymentNoteValidator.looksLikeCardNumber(null)).isFalse();
    }

    @Test
    void emptyNote_isNotFlagged() {
        assertThat(PaymentNoteValidator.looksLikeCardNumber("")).isFalse();
    }
}
