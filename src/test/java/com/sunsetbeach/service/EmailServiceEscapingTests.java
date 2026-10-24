package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Audit finding H4: guest-typed values must reach an email's HTML as text, never as markup. */
class EmailServiceEscapingTests {

    @Test
    void markupInAGuestTypedValue_isEscaped() {
        assertThat(EmailService.esc("<a href=\"https://evil\">Confirm</a>"))
                .isEqualTo("&lt;a href=&quot;https://evil&quot;&gt;Confirm&lt;/a&gt;");
    }

    @Test
    void aMissingValue_isEmpty_notTheWordNull() {
        assertThat(EmailService.esc(null)).isEmpty();
    }
}
