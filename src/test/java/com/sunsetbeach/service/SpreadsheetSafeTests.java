package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Formula injection in exports: a cell a spreadsheet would run as a formula leaves this app with a
 * leading apostrophe, in every export (CSV through {@link CsvBuilder}, workbooks through {@link
 * SpreadsheetSafe#sanitize}), and this app's own roster re-import gets the original text back.
 */
class SpreadsheetSafeTests {

    @ParameterizedTest
    @ValueSource(strings = {"=1+1", "=HYPERLINK(\"http://evil\",\"x\")", "+cmd|' /C calc'!A0", "-2+3", "@SUM(A1)", "\tTabbed", "\rReturned", "=", "-", "+"})
    void text_startingLikeAFormula_getsALeadingApostrophe(String value) {
        assertThat(SpreadsheetSafe.text(value)).isEqualTo("'" + value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Jane Doe", "", "9", "08:00", "a=b", "x+y", "Ocean View - Suite", "'already quoted", "#1204"})
    void text_otherwise_isUntouched(String value) {
        assertThat(SpreadsheetSafe.text(value)).isEqualTo(value);
    }

    @ParameterizedTest
    @ValueSource(strings = {"-50.00", "+66812345678", "-1", "+0.5", "-3,5"})
    void text_aPlainNumber_staysANumber(String value) {
        // It cannot be a formula, and its sign is data (a negative discrepancy, a phone number in
        // international form) - escaping it would turn an amount into text.
        assertThat(SpreadsheetSafe.text(value)).isEqualTo(value);
    }

    @Test
    void text_null_staysNull() {
        assertThat(SpreadsheetSafe.text(null)).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"=1+1", "+cmd", "-x", "@y", "\tz"})
    void unescape_undoesText(String value) {
        assertThat(SpreadsheetSafe.unescape(SpreadsheetSafe.text(value))).isEqualTo(value);
    }

    @Test
    void unescape_leavesATextThatMerelyStartsWithAnApostropheAlone() {
        assertThat(SpreadsheetSafe.unescape("'hello")).isEqualTo("'hello");
        assertThat(SpreadsheetSafe.unescape("'")).isEqualTo("'");
        assertThat(SpreadsheetSafe.unescape("plain")).isEqualTo("plain");
    }

    @Test
    void csvBuilder_escapesEveryDangerousCell_thenQuotesAsCsv() {
        String csv = new CsvBuilder().row("Guest", "Note").row("=cmd|' /C calc'!A0", "-5.00").row("@evil, with comma", "+66 81 234").toString();

        assertThat(csv).contains("\r\n'=cmd|' /C calc'!A0,-5.00");
        assertThat(csv).contains("\"'@evil, with comma\",'+66 81 234");
    }

    @Test
    void sanitize_rewritesTextCellsOnEverySheet_andLeavesNumbersAndSafeText() throws Exception {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet first = workbook.createSheet("First");
            first.createRow(0).createCell(0).setCellValue("=SUM(A2:A3)");
            first.getRow(0).createCell(1).setCellValue("Jane");
            first.getRow(0).createCell(2).setCellValue(-12.5);
            Sheet hidden = workbook.createSheet("Hidden");
            hidden.createRow(0).createCell(0).setCellValue("@HYPERLINK(1)");

            SpreadsheetSafe.sanitize(workbook);

            assertThat(first.getRow(0).getCell(0).getStringCellValue()).isEqualTo("'=SUM(A2:A3)");
            assertThat(first.getRow(0).getCell(1).getStringCellValue()).isEqualTo("Jane");
            Cell number = first.getRow(0).getCell(2);
            assertThat(number.getNumericCellValue()).isEqualTo(-12.5);
            assertThat(hidden.getRow(0).getCell(0).getStringCellValue()).isEqualTo("'@HYPERLINK(1)");
        }
    }
}
