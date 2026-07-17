package com.sunsetbeach.mapper;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Prisma's Decimal serializes to JSON as a fixed-scale string (e.g. "1500.00"), which is
 * what Room.basePrice/Booking.totalPrice use. The pricing endpoint instead does an explicit
 * JS {@code Number(...)} conversion, which drops trailing zeros (1500.00 -> 1500) - that's
 * what asRealNumber replicates.
 */
public final class PriceFormat {

    private PriceFormat() {
    }

    public static String asDecimalString(BigDecimal value) {
        return value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    public static BigDecimal asRealNumber(BigDecimal value) {
        BigDecimal stripped = value.stripTrailingZeros();
        // stripTrailingZeros() can yield a negative scale (e.g. 1500 -> unscaled 15, scale -2),
        // which BigDecimal#toString renders in scientific notation; round-trip through the
        // plain-string form so the result always has scale >= 0 and prints like JS's Number().
        return new BigDecimal(stripped.toPlainString());
    }
}
