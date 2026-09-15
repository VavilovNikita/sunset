package com.sunsetbeach.rosterimport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.model.FillColor;
import com.sunsetbeach.model.StaffArea;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * {@link ScheduleWorkbookParser} against the hotel's own real file (four real months, not a
 * fixture built to be easy) - src/test/resources/rosterimport/sample-schedule.xlsx, the same file
 * this importer was commissioned to read. The expected counts below were independently computed
 * with openpyxl (not this parser) reading the same file, specifically so this test can't pass by
 * sharing a bug with the code it's checking.
 */
class ScheduleWorkbookParserTests {

    private final ScheduleWorkbookParser parser = new ScheduleWorkbookParser();

    private InputStream sample() {
        InputStream in = getClass().getResourceAsStream("/rosterimport/sample-schedule.xlsx");
        assertThat(in).as("sample-schedule.xlsx must be on the test classpath").isNotNull();
        return in;
    }

    /** rawCode, or "9-YELLOW"/"9-BLUE" for the ambiguous one - counted independently with openpyxl against the same file. */
    private static Map<String, Long> countByCode(List<ParsedCell> cells) {
        return cells.stream().collect(Collectors.groupingBy(
                c -> "9".equals(c.rawCode()) ? "9-" + c.fillColor() : c.rawCode(), Collectors.counting()));
    }

    @Test
    void sep26_matchesIndependentlyCountedCodesExactly() throws IOException {
        try (InputStream in = sample()) {
            ParsedSchedule result = parser.parse(in, 2026, 9);

            assertThat(result.issues()).isEmpty();
            assertThat(result.cells()).hasSize(409);
            Map<String, Long> counts = countByCode(result.cells());
            Map<String, Long> expected = new HashMap<>();
            expected.put("12", 58L);
            expected.put("7", 130L);
            expected.put("8", 32L);
            expected.put("8.3", 1L);
            expected.put("9-BLUE", 20L);
            expected.put("9-YELLOW", 24L);
            expected.put("OP", 76L);
            expected.put("PH", 61L);
            expected.put("PT1", 5L);
            expected.put("PT3", 1L);
            expected.put("SS", 1L);
            assertThat(counts).isEqualTo(expected);

            long distinctNames = result.cells().stream().map(ParsedCell::rawName).distinct().count();
            assertThat(distinctNames).isEqualTo(18);
        }
    }

    @Test
    void jun26_matchesIndependentlyCountedCodesExactly() throws IOException {
        try (InputStream in = sample()) {
            ParsedSchedule result = parser.parse(in, 2026, 6);

            assertThat(result.issues()).isEmpty();
            assertThat(result.cells()).hasSize(258);
            Map<String, Long> counts = countByCode(result.cells());
            assertThat(counts.get("9-BLUE")).isEqualTo(27L);
            assertThat(counts.get("9-YELLOW")).isEqualTo(3L);
            assertThat(counts.get("10")).isEqualTo(23L);
            assertThat(counts.get("11")).isEqualTo(1L);
        }
    }

    @Test
    void jul26_matchesIndependentlyCountedCodesExactly() throws IOException {
        try (InputStream in = sample()) {
            ParsedSchedule result = parser.parse(in, 2026, 7);

            assertThat(result.issues()).isEmpty();
            assertThat(result.cells()).hasSize(369);
            Map<String, Long> counts = countByCode(result.cells());
            assertThat(counts.get("9-BLUE")).isEqualTo(34L);
            assertThat(counts.get("9-YELLOW")).isEqualTo(19L);
            assertThat(counts.get("PT1")).isEqualTo(45L);
        }
    }

    @Test
    void aug26_matchesIndependentlyCountedCodesExactly() throws IOException {
        try (InputStream in = sample()) {
            ParsedSchedule result = parser.parse(in, 2026, 8);

            assertThat(result.issues()).isEmpty();
            assertThat(result.cells()).hasSize(446);
            Map<String, Long> counts = countByCode(result.cells());
            // August's grid never uses the yellow "9" at all - only the split one.
            assertThat(counts.get("9-YELLOW")).isNull();
            assertThat(counts.get("9-BLUE")).isEqualTo(16L);
        }
    }

    /**
     * One specific, hand-verified cell: Namtan sits under "Front Office." in September, and D9
     * (day 1) is a yellow 9 - the single 09:00-18:00 shift, not the split one Kitchen/Restaurant
     * use the same digit for two rows below.
     */
    @Test
    void sep26_specificCell_namtanDay1_isYellowNineUnderFrontOffice() throws IOException {
        try (InputStream in = sample()) {
            ParsedSchedule result = parser.parse(in, 2026, 9);

            ParsedCell cell = result.cells().stream()
                    .filter(c -> c.rawName().equals("Namtan") && c.date().equals(LocalDate.of(2026, 9, 1)))
                    .findFirst()
                    .orElseThrow();
            assertThat(cell.staffArea()).isEqualTo(StaffArea.FRONT_OFFICE);
            assertThat(cell.rawCode()).isEqualTo("9");
            assertThat(cell.fillColor()).isEqualTo(FillColor.YELLOW);
            assertThat(cell.cellRef()).isEqualTo("D9");
        }
    }

    /** Kitchen's Tong uses the split "9" (light blue) under the very same digit Front Office reads as single - the reason colour, not department, has to decide. */
    @Test
    void jun26_specificCell_kitchenNineIsBlueNotYellow() throws IOException {
        try (InputStream in = sample()) {
            ParsedSchedule result = parser.parse(in, 2026, 6);

            ParsedCell cell = result.cells().stream()
                    .filter(c -> c.rawName().equals("Tong") && c.cellRef().equals("I21"))
                    .findFirst()
                    .orElseThrow();
            assertThat(cell.staffArea()).isEqualTo(StaffArea.KITCHEN);
            assertThat(cell.fillColor()).isEqualTo(FillColor.BLUE);
        }
    }

    @Test
    void syntheticFile_recognisesBothNineFillsAndTellsThemApart() throws IOException {
        InputStream in = new ScheduleWorkbookBuilder("Jan26", 1, 2)
                .department("Front Office")
                .person("Alice", "9", "PH")
                .nineFill("Alice", 0, true)
                .department("Kitchen")
                .person("Bob", "9", "OP")
                .nineFill("Bob", 0, false)
                .stopMarker()
                .build();

        ParsedSchedule result = parser.parse(in, 2026, 1);

        assertThat(result.issues()).isEmpty();
        ParsedCell alice = result.cells().stream().filter(c -> c.rawName().equals("Alice") && "9".equals(c.rawCode())).findFirst().orElseThrow();
        assertThat(alice.fillColor()).isEqualTo(FillColor.YELLOW);
        assertThat(alice.staffArea()).isEqualTo(StaffArea.FRONT_OFFICE);
        ParsedCell bob = result.cells().stream().filter(c -> c.rawName().equals("Bob") && "9".equals(c.rawCode())).findFirst().orElseThrow();
        assertThat(bob.fillColor()).isEqualTo(FillColor.BLUE);
        assertThat(bob.staffArea()).isEqualTo(StaffArea.KITCHEN);
    }

    @Test
    void syntheticFile_nineWithUnrecognisedFill_isReportedNotGuessed() throws IOException {
        InputStream in = new ScheduleWorkbookBuilder("Jan26", 1)
                .department("Front Office")
                .person("Alice", "9")
                .unrecognisedFill("Alice", 0)
                .stopMarker()
                .build();

        ParsedSchedule result = parser.parse(in, 2026, 1);

        assertThat(result.cells()).isEmpty();
        assertThat(result.issues()).hasSize(1);
        assertThat(result.issues().get(0).message()).contains("neither the yellow nor the light-blue fill");
    }

    @Test
    void syntheticFile_unrecognisedDepartment_reportsAndContinuesWithoutCrashing() throws IOException {
        InputStream in = new ScheduleWorkbookBuilder("Jan26", 1, 2)
                .department("Spa") // not one of the six known departments
                .person("Alice", "7", null)
                .department("Kitchen")
                .person("Bob", "OP", "7")
                .stopMarker()
                .build();

        ParsedSchedule result = parser.parse(in, 2026, 1);

        assertThat(result.issues()).hasSize(2); // Alice's own department, and her one coded cell under it
        assertThat(result.cells()).hasSize(2); // Bob's two cells still parse fine
        assertThat(result.cells()).allMatch(c -> c.rawName().equals("Bob"));
    }

    @Test
    void unknownMonth_noMatchingSheet_throwsWithAvailableSheetNames() throws IOException {
        try (InputStream in = sample()) {
            assertThatThrownBy(() -> parser.parse(in, 2026, 1))
                    .isInstanceOf(ScheduleParseException.class)
                    .hasMessageContaining("Jan26")
                    .hasMessageContaining("Sep26");
        }
    }
}
