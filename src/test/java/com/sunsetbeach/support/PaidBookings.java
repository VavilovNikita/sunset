package com.sunsetbeach.support;

import com.sunsetbeach.model.BookingFolio;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.FolioPaymentInput;
import com.sunsetbeach.model.FolioPaymentMethod;
import com.sunsetbeach.service.BookingService;
import java.math.BigDecimal;

/**
 * A change into {@code PAID} has to carry a payment for what is owed (see {@code
 * BookingService#settleForPaid}); tests that only need "a booking that is PAID" build the request
 * the way the desk does - paying exactly the server's {@code amountToMarkPaid} in cash. The caller
 * needs a {@code StaffPrincipal} in the security context, like any folio payment.
 */
public final class PaidBookings {

    private PaidBookings() {}

    public static BookingStatusInput paidInput(BookingService bookingService, String bookingId) {
        BookingFolio folio = bookingService.getFolio(bookingId);
        BookingStatusInput input = new BookingStatusInput(BookingStatus.PAID);
        if (new BigDecimal(folio.getAmountToMarkPaid()).signum() > 0) {
            input.payment(new FolioPaymentInput(FolioPaymentMethod.CASH, folio.getAmountToMarkPaid()));
        }
        return input;
    }
}
