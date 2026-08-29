package com.sunsetbeach.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Heuristic guard against staff pasting a full card number into {@code Booking.paymentNote}
 * (free text, read by any CASHIER one booking at a time and by any MANAGER in bulk via
 * {@code GET /bookings/export}) - this app has no field meant to hold card numbers at all, so
 * one showing up here is always a mistake, not a legitimate use of the field.
 *
 * <p>Flags a digit run of card-number length (13-19 digits, spaces/dashes allowed as separators)
 * only if it also passes the Luhn checksum - a plain digit-count check would also reject
 * legitimate booking/receipt reference numbers of similar length, which fail Luhn at roughly a
 * 9-in-10 rate purely by chance. This is still a heuristic, not a guarantee: a real card number
 * that happens to fail Luhn (extremely unlikely - Luhn is exactly the checksum card numbers are
 * required to satisfy) would slip through, and a made-up reference number that happens to pass
 * Luhn (roughly 1-in-10) would be rejected as a false positive.
 */
final class PaymentNoteValidator {

    private static final Pattern DIGIT_RUN = Pattern.compile("(?:\\d[ -]?){13,19}");

    private PaymentNoteValidator() {
    }

    static boolean looksLikeCardNumber(String note) {
        if (note == null) {
            return false;
        }
        Matcher matcher = DIGIT_RUN.matcher(note);
        while (matcher.find()) {
            String digits = matcher.group().replaceAll("[ -]", "");
            if (digits.length() >= 13 && digits.length() <= 19 && passesLuhn(digits)) {
                return true;
            }
        }
        return false;
    }

    private static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean alternate = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int n = digits.charAt(i) - '0';
            if (alternate) {
                n *= 2;
                if (n > 9) {
                    n -= 9;
                }
            }
            sum += n;
            alternate = !alternate;
        }
        return sum % 10 == 0;
    }
}
