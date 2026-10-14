package com.sunsetbeach.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NameTextTests {

    @Test
    void strip_removesEveryUnicodeSpaceAndZeroWidthCharacterAtTheEdges() {
        assertThat(NameText.strip("\u00a0 \u2003\u200bAnna Smith\ufeff\u202f\t")).isEqualTo("Anna Smith");
        assertThat(NameText.strip("\u00a0\u200b\u3000")).isEmpty();
        assertThat(NameText.strip(null)).isNull();
    }

    @Test
    void strip_keepsInnerCharactersAndRealLetters() {
        assertThat(NameText.strip("  A\u00a0B  ")).isEqualTo("A\u00a0B");
        assertThat(NameText.strip("Zoë")).isEqualTo("Zoë");
    }

    @Test
    void collapse_turnsRunsOfAnyBlankIntoOneSpace() {
        assertThat(NameText.collapse("  Standard \u00a0\u00a0  Room ")).isEqualTo("Standard Room");
    }
}
