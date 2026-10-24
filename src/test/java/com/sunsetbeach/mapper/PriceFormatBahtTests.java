package com.sunsetbeach.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PriceFormatBahtTests {

    @Test
    void wholeAmounts_haveNoSatang_andThousandsAreGrouped() {
        assertThat(PriceFormat.baht(new BigDecimal("1000.00"))).isEqualTo("฿1,000");
        assertThat(PriceFormat.baht(new BigDecimal("0.00"))).isEqualTo("฿0");
        assertThat(PriceFormat.baht(new BigDecimal("1234567"))).isEqualTo("฿1,234,567");
    }

    @Test
    void fractionsKeepTwoDecimals_andNegativesKeepTheirSign() {
        assertThat(PriceFormat.baht(new BigDecimal("1000.5"))).isEqualTo("฿1,000.50");
        assertThat(PriceFormat.baht(new BigDecimal("-50.00"))).isEqualTo("-฿50");
    }
}
