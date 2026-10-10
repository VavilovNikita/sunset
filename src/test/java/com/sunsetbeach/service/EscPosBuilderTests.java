package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.model.PrinterCodepage;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** What reaches the printer: no "?" where a word processor's dash or quote used to be. */
class EscPosBuilderTests {

    @Test
    void typographicPunctuationBecomesAscii_insteadOfAQuestionMarkOnPaper() {
        assertThat(EscPosBuilder.asciiPunctuation("Restaurant \u2014 T1")).isEqualTo("Restaurant - T1");
        assertThat(EscPosBuilder.asciiPunctuation("5\u201310")).isEqualTo("5-10");
        assertThat(EscPosBuilder.asciiPunctuation("\u201Cno ice\u201D, don\u2019t")).isEqualTo("\"no ice\", don't");
        assertThat(EscPosBuilder.asciiPunctuation("wait\u2026")).isEqualTo("wait...");
    }

    @Test
    void theBytesSentCarryTheDash_notAQuestionMark() {
        byte[] payload = new EscPosBuilder(PrinterCodepage.PC437).line("Restaurant \u2014 T1").cutAndBuild();

        assertThat(new String(payload, StandardCharsets.ISO_8859_1)).contains("Restaurant - T1").doesNotContain("Restaurant ? T1");
    }
}
