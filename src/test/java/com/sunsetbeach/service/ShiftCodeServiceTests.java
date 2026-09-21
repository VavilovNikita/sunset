package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.RosterImportShiftColorMappingEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.FillColor;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.repository.RosterImportShiftColorMappingRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Shift codes are versioned, never edited - see V57__staff_area_and_shift_code.sql and
 * ShiftCodeService's own javadoc. These tests cover the versioning (creating a new one retires
 * the old, without touching what's already there) and the interval validation, not the drift
 * scenario itself (that's a fact about the source spreadsheet, not something to reproduce here).
 */
@SpringBootTest
class ShiftCodeServiceTests extends AbstractIntegrationTest {

    @Autowired
    private ShiftCodeService shiftCodeService;

    @Autowired
    private ShiftCodeRepository shiftCodeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RosterImportShiftColorMappingRepository colorMappingRepository;

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdShiftCodeIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        colorMappingRepository.deleteAll();
        shiftCodeRepository.deleteAllById(createdShiftCodeIds);
        createdUserIds.forEach(userRepository::deleteById);
    }

    private UserEntity createManager() {
        UserEntity user = new UserEntity();
        user.setEmail("shift-code-manager-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(Role.MANAGER);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    @Test
    void create_newVersion_retiresThePreviousOneButKeepsItById() {
        UserEntity manager = createManager();
        String code = "TEST" + UUID.randomUUID().toString().substring(0, 6);

        ShiftCode first = shiftCodeService.create(
                new ShiftCodeCreateInput(code, ShiftCodeKind.MORNING, true, true, "2026-01-01").staffArea(StaffArea.RESTAURANT).startTime1("09:00").endTime1("17:00"),
                manager.getId());
        createdShiftCodeIds.add(first.getId());
        assertThat(first.getActive()).isTrue();

        ShiftCode second = shiftCodeService.create(
                new ShiftCodeCreateInput(code, ShiftCodeKind.MORNING, true, true, "2026-02-01").staffArea(StaffArea.RESTAURANT).startTime1("10:00").endTime1("18:00"),
                manager.getId());
        createdShiftCodeIds.add(second.getId());

        List<ShiftCode> active = shiftCodeService.list(StaffArea.RESTAURANT);
        assertThat(active).extracting(ShiftCode::getId).contains(second.getId()).doesNotContain(first.getId());
        // The retired version still exists, unchanged, for any RosterEntry already pointing at it.
        assertThat(shiftCodeRepository.findById(first.getId()).orElseThrow().isActive()).isFalse();
        assertThat(shiftCodeRepository.findById(first.getId()).orElseThrow().getStartTime1().toString()).isEqualTo("09:00");
    }

    @Test
    void create_sameCodeDifferentArea_bothStayActiveIndependently() {
        UserEntity manager = createManager();
        String code = "TEST" + UUID.randomUUID().toString().substring(0, 6);

        ShiftCode restaurantVersion = shiftCodeService.create(
                new ShiftCodeCreateInput(code, ShiftCodeKind.SPLIT, true, true, "2026-01-01").staffArea(StaffArea.RESTAURANT).startTime1("09:00").endTime1("13:00").startTime2("16:00").endTime2("21:00"),
                manager.getId());
        createdShiftCodeIds.add(restaurantVersion.getId());
        ShiftCode frontOfficeVersion = shiftCodeService.create(
                new ShiftCodeCreateInput(code, ShiftCodeKind.MORNING, true, true, "2026-01-01").staffArea(StaffArea.FRONT_OFFICE).startTime1("09:00").endTime1("18:00"),
                manager.getId());
        createdShiftCodeIds.add(frontOfficeVersion.getId());

        assertThat(shiftCodeService.list(StaffArea.RESTAURANT)).extracting(ShiftCode::getId).contains(restaurantVersion.getId());
        assertThat(shiftCodeService.list(StaffArea.FRONT_OFFICE)).extracting(ShiftCode::getId).contains(frontOfficeVersion.getId());
        // Same code string, genuinely different hours - area-scoping stays fully supported even
        // though no code at this hotel actually relies on it today (see ShiftCode's own doc).
        assertThat(restaurantVersion.getStartTime2().get()).isEqualTo("16:00");
        assertThat(frontOfficeVersion.getStartTime2().isPresent()).isFalse();
    }

    @Test
    void create_secondIntervalWithoutFirst_isRejected() {
        UserEntity manager = createManager();
        assertThatThrownBy(() -> shiftCodeService.create(
                        new ShiftCodeCreateInput("BAD" + UUID.randomUUID().toString().substring(0, 4), ShiftCodeKind.SPLIT, true, true, "2026-01-01")
                                .staffArea(StaffArea.KITCHEN)
                                .startTime2("18:00")
                                .endTime2("21:00"),
                        manager.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_endBeforeStart_isRejected() {
        UserEntity manager = createManager();
        assertThatThrownBy(() -> shiftCodeService.create(
                        new ShiftCodeCreateInput("BAD" + UUID.randomUUID().toString().substring(0, 4), ShiftCodeKind.EVENING, true, true, "2026-01-01")
                                .staffArea(StaffArea.KITCHEN)
                                .startTime1("18:00")
                                .endTime1("09:00"),
                        manager.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_zeroIntervals_isOpAndCountsAsWorked() {
        UserEntity manager = createManager();
        ShiftCode op = shiftCodeService.create(
                new ShiftCodeCreateInput("OP" + UUID.randomUUID().toString().substring(0, 4), ShiftCodeKind.OPEN_SCHEDULE, true, true, "2026-01-01").staffArea(StaffArea.MAINTENANCE),
                manager.getId());
        createdShiftCodeIds.add(op.getId());

        assertThat(op.getCountsAsWorked()).isTrue();
        assertThat(op.getStartTime1().isPresent()).isFalse();
        assertThat(op.getStartTime2().isPresent()).isFalse();
    }

    @Test
    void create_withNoStaffArea_isSharedAcrossEveryArea() {
        UserEntity manager = createManager();
        String code = "SHARED" + UUID.randomUUID().toString().substring(0, 6);

        ShiftCode shared = shiftCodeService.create(
                new ShiftCodeCreateInput(code, ShiftCodeKind.MORNING, true, true, "2026-01-01").startTime1("09:00").endTime1("17:00"),
                manager.getId());
        createdShiftCodeIds.add(shared.getId());

        assertThat(shared.getStaffArea().isPresent()).isFalse();
        assertThat(shiftCodeService.list(StaffArea.RESTAURANT)).extracting(ShiftCode::getId).contains(shared.getId());
        assertThat(shiftCodeService.list(StaffArea.KITCHEN)).extracting(ShiftCode::getId).contains(shared.getId());
        assertThat(shiftCodeService.list(StaffArea.FRONT_OFFICE)).extracting(ShiftCode::getId).contains(shared.getId());
    }

    /**
     * The scenario the correction named directly: an area-scoped code defined later, colliding
     * with an existing shared code of the same `code` string. The area-scoped one wins for its
     * own area (the shared row is left out of that area's resolved list, not merely appended
     * alongside it) while every other area keeps seeing the shared version, unaffected - and
     * neither row retires the other, since {@code staffArea} is part of what identifies "the same
     * code" (see ShiftCodeRepository#findByStaffAreaAndCodeAndActiveTrue's own comment).
     */
    @Test
    void create_areaScopedCodeCollidingWithSharedCode_winsForItsOwnAreaOnly() {
        UserEntity manager = createManager();
        String code = "COLLIDE" + UUID.randomUUID().toString().substring(0, 6);

        ShiftCode shared = shiftCodeService.create(
                new ShiftCodeCreateInput(code, ShiftCodeKind.MORNING, true, true, "2026-01-01").startTime1("09:00").endTime1("17:00"),
                manager.getId());
        createdShiftCodeIds.add(shared.getId());

        ShiftCode restaurantOverride = shiftCodeService.create(
                new ShiftCodeCreateInput(code, ShiftCodeKind.SPLIT, true, true, "2026-01-01").staffArea(StaffArea.RESTAURANT).startTime1("09:00").endTime1("13:00").startTime2("16:00").endTime2("21:00"),
                manager.getId());
        createdShiftCodeIds.add(restaurantOverride.getId());

        // Restaurant sees only the area-scoped override for this code - not both rows.
        List<ShiftCode> restaurantList = shiftCodeService.list(StaffArea.RESTAURANT);
        assertThat(restaurantList).filteredOn(c -> c.getCode().equals(code)).extracting(ShiftCode::getId).containsExactly(restaurantOverride.getId());

        // Every other area still sees the shared version, untouched by the override.
        List<ShiftCode> kitchenList = shiftCodeService.list(StaffArea.KITCHEN);
        assertThat(kitchenList).filteredOn(c -> c.getCode().equals(code)).extracting(ShiftCode::getId).containsExactly(shared.getId());

        // Neither row retired the other - both still active, independently.
        assertThat(shiftCodeRepository.findById(shared.getId()).orElseThrow().isActive()).isTrue();
        assertThat(shiftCodeRepository.findById(restaurantOverride.getId()).orElseThrow().isActive()).isTrue();

        // The unfiltered "every code as stored" view shows both rows - the collision itself,
        // for the shift-code management screen, not a resolved-for-one-area picker.
        List<ShiftCode> everything = shiftCodeService.list(null);
        assertThat(everything).filteredOn(c -> c.getCode().equals(code)).extracting(ShiftCode::getId)
                .containsExactlyInAnyOrder(shared.getId(), restaurantOverride.getId());
    }

    @Test
    void create_kindMismatchedWithShape_isRejected() {
        UserEntity manager = createManager();
        // SPLIT claimed for a code with only one interval - the shape says otherwise.
        assertThatThrownBy(() -> shiftCodeService.create(
                        new ShiftCodeCreateInput("BAD" + UUID.randomUUID().toString().substring(0, 4), ShiftCodeKind.SPLIT, true, true, "2026-01-01")
                                .staffArea(StaffArea.KITCHEN)
                                .startTime1("09:00")
                                .endTime1("17:00"),
                        manager.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_absenceThatCountsAsWorked_isRejected() {
        UserEntity manager = createManager();
        // ABSENCE must not count as worked - the whole point of the kind is "not working".
        assertThatThrownBy(() -> shiftCodeService.create(
                        new ShiftCodeCreateInput("BAD" + UUID.randomUUID().toString().substring(0, 4), ShiftCodeKind.ABSENCE, true, true, "2026-01-01")
                                .staffArea(StaffArea.KITCHEN),
                        manager.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void updateKind_correctsInPlace_sameRowSameVersion() {
        UserEntity manager = createManager();
        ShiftCode code = shiftCodeService.create(
                new ShiftCodeCreateInput("K" + UUID.randomUUID().toString().substring(0, 4), ShiftCodeKind.MORNING, true, true, "2026-01-01")
                        .staffArea(StaffArea.KITCHEN)
                        .startTime1("09:00")
                        .endTime1("17:00"),
                manager.getId());
        createdShiftCodeIds.add(code.getId());

        ShiftCode corrected = shiftCodeService.updateKind(code.getId(), ShiftCodeKind.EVENING, manager.getId());

        // Same row, same id, still active - a kind correction is not a new version (contrast
        // create_newVersion_retiresThePreviousOneButKeepsItById, above).
        assertThat(corrected.getId()).isEqualTo(code.getId());
        assertThat(corrected.getKind().get()).isEqualTo(ShiftCodeKind.EVENING);
        assertThat(shiftCodeRepository.findById(code.getId()).orElseThrow().isActive()).isTrue();
        assertThat(shiftCodeService.list(StaffArea.KITCHEN)).extracting(ShiftCode::getId).contains(code.getId());
    }

    @Test
    void updateKind_mismatchedWithShape_isRejected() {
        UserEntity manager = createManager();
        ShiftCode code = shiftCodeService.create(
                new ShiftCodeCreateInput("K" + UUID.randomUUID().toString().substring(0, 4), ShiftCodeKind.MORNING, true, true, "2026-01-01")
                        .staffArea(StaffArea.KITCHEN)
                        .startTime1("09:00")
                        .endTime1("17:00"),
                manager.getId());
        createdShiftCodeIds.add(code.getId());

        assertThatThrownBy(() -> shiftCodeService.updateKind(code.getId(), ShiftCodeKind.OPEN_SCHEDULE, manager.getId()))
                .isInstanceOf(BadRequestException.class);
        // Refused, not silently applied - the row still carries its original, valid kind.
        assertThat(shiftCodeRepository.findById(code.getId()).orElseThrow().getKind()).isEqualTo(ShiftCodeKind.MORNING);
    }

    @Test
    void list_legacyRowWithNoKind_suggestsFromItsOwnShape() {
        // Simulates a row that predates the kind column - never possible through create() itself
        // now that kind is required there, but exactly the shape a pre-existing production row
        // has until confirmed via PATCH /shift-codes/{id}/kind.
        UserEntity manager = createManager();
        com.sunsetbeach.entity.ShiftCodeEntity entity = new com.sunsetbeach.entity.ShiftCodeEntity();
        entity.setCode("LEGACY" + UUID.randomUUID().toString().substring(0, 6));
        entity.setStaffArea(StaffArea.MAINTENANCE);
        entity.setStartTime1(java.time.LocalTime.of(16, 0));
        entity.setEndTime1(java.time.LocalTime.of(22, 0));
        entity.setCountsAsWorked(true);
        entity.setPaid(true);
        entity.setEffectiveFrom(java.time.LocalDate.of(2026, 1, 1));
        entity.setCreatedByUserId(manager.getId());
        com.sunsetbeach.entity.ShiftCodeEntity saved = shiftCodeRepository.saveAndFlush(entity);
        createdShiftCodeIds.add(saved.getId());

        ShiftCode dto = shiftCodeService.list(StaffArea.MAINTENANCE).stream()
                .filter(c -> c.getId().equals(saved.getId()))
                .findFirst()
                .orElseThrow();
        assertThat(dto.getKind().isPresent()).isFalse();
        // 16:00 is afternoon/evening, not before noon - the suggestion reads the real clock time.
        assertThat(dto.getSuggestedKind().get()).isEqualTo(ShiftCodeKind.EVENING);
    }

    @Test
    void updateDisplayColor_setsThenClearsInPlace_sameRowSameVersion() {
        UserEntity manager = createManager();
        ShiftCode code = shiftCodeService.create(
                new ShiftCodeCreateInput("C" + UUID.randomUUID().toString().substring(0, 4), ShiftCodeKind.MORNING, true, true, "2026-01-01")
                        .staffArea(StaffArea.KITCHEN)
                        .startTime1("09:00")
                        .endTime1("17:00"),
                manager.getId());
        createdShiftCodeIds.add(code.getId());
        assertThat(code.getDisplayColor().isPresent()).isFalse();

        ShiftCode colored = shiftCodeService.updateDisplayColor(code.getId(), "#3B82F6", manager.getId());
        assertThat(colored.getId()).isEqualTo(code.getId());
        assertThat(colored.getDisplayColor().get()).isEqualTo("#3B82F6");
        assertThat(shiftCodeRepository.findById(code.getId()).orElseThrow().isActive()).isTrue();

        // Unlike kind, this may be cleared back to unset - there's no "unconfirmed" state to
        // protect here.
        ShiftCode cleared = shiftCodeService.updateDisplayColor(code.getId(), null, manager.getId());
        assertThat(shiftCodeRepository.findById(code.getId()).orElseThrow().getDisplayColor()).isNull();
        assertThat(cleared.getDisplayColor().isPresent()).isFalse();
    }

    @Test
    void list_forNineOrNineS_suggestsColorFromTheColorMapping_everyOtherCodeGetsNone() {
        UserEntity manager = createManager();
        ShiftCode nine = shiftCodeService.create(
                new ShiftCodeCreateInput("9", ShiftCodeKind.MORNING, true, true, "2026-01-01").startTime1("09:00").endTime1("18:00"), manager.getId());
        createdShiftCodeIds.add(nine.getId());
        ShiftCode ordinary = shiftCodeService.create(
                new ShiftCodeCreateInput("OP" + UUID.randomUUID().toString().substring(0, 4), ShiftCodeKind.OPEN_SCHEDULE, true, true, "2026-01-01"),
                manager.getId());
        createdShiftCodeIds.add(ordinary.getId());

        RosterImportShiftColorMappingEntity mapping = new RosterImportShiftColorMappingEntity();
        mapping.setRawCode("9");
        mapping.setFillColor(FillColor.YELLOW);
        mapping.setResolvedCode("9");
        mapping.setCreatedByUserId(manager.getId());
        colorMappingRepository.saveAndFlush(mapping);

        List<ShiftCode> all = shiftCodeService.list(null);
        ShiftCode nineDto = all.stream().filter(c -> c.getId().equals(nine.getId())).findFirst().orElseThrow();
        ShiftCode ordinaryDto = all.stream().filter(c -> c.getId().equals(ordinary.getId())).findFirst().orElseThrow();

        // Yellow "9" is the single 09:00-18:00 shift - see ShiftCode's own openapi.yaml
        // description for why this hex is exact, not an approximation, for YELLOW specifically.
        assertThat(nineDto.getSuggestedColor().get()).isEqualTo("#FFFF00");
        // Nothing else in the source spreadsheet was ever told apart by cell colour.
        assertThat(ordinaryDto.getSuggestedColor().isPresent()).isFalse();

        // Once an admin has actually picked a colour, the suggestion is no longer offered -
        // same "suggest, don't silently apply" precedent as suggestedKind.
        shiftCodeService.updateDisplayColor(nine.getId(), "#123456", manager.getId());
        ShiftCode nineAfterColorChosen = shiftCodeService.list(null).stream()
                .filter(c -> c.getId().equals(nine.getId())).findFirst().orElseThrow();
        assertThat(nineAfterColorChosen.getSuggestedColor().isPresent()).isFalse();
    }
}
