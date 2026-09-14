package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.StaffArea;
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

    private final List<String> createdUserIds = new ArrayList<>();
    private final List<String> createdShiftCodeIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
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
                new ShiftCodeCreateInput(code, true, true, "2026-01-01").staffArea(StaffArea.RESTAURANT).startTime1("09:00").endTime1("17:00"),
                manager.getId());
        createdShiftCodeIds.add(first.getId());
        assertThat(first.getActive()).isTrue();

        ShiftCode second = shiftCodeService.create(
                new ShiftCodeCreateInput(code, true, true, "2026-02-01").staffArea(StaffArea.RESTAURANT).startTime1("10:00").endTime1("18:00"),
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
                new ShiftCodeCreateInput(code, true, true, "2026-01-01").staffArea(StaffArea.RESTAURANT).startTime1("09:00").endTime1("13:00").startTime2("16:00").endTime2("21:00"),
                manager.getId());
        createdShiftCodeIds.add(restaurantVersion.getId());
        ShiftCode frontOfficeVersion = shiftCodeService.create(
                new ShiftCodeCreateInput(code, true, true, "2026-01-01").staffArea(StaffArea.FRONT_OFFICE).startTime1("09:00").endTime1("18:00"),
                manager.getId());
        createdShiftCodeIds.add(frontOfficeVersion.getId());

        assertThat(shiftCodeService.list(StaffArea.RESTAURANT)).extracting(ShiftCode::getId).contains(restaurantVersion.getId());
        assertThat(shiftCodeService.list(StaffArea.FRONT_OFFICE)).extracting(ShiftCode::getId).contains(frontOfficeVersion.getId());
        // Same code string, genuinely different hours - the ambiguity the source spreadsheet has for "9".
        assertThat(restaurantVersion.getStartTime2().get()).isEqualTo("16:00");
        assertThat(frontOfficeVersion.getStartTime2().isPresent()).isFalse();
    }

    @Test
    void create_secondIntervalWithoutFirst_isRejected() {
        UserEntity manager = createManager();
        assertThatThrownBy(() -> shiftCodeService.create(
                        new ShiftCodeCreateInput("BAD" + UUID.randomUUID().toString().substring(0, 4), true, true, "2026-01-01")
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
                        new ShiftCodeCreateInput("BAD" + UUID.randomUUID().toString().substring(0, 4), true, true, "2026-01-01")
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
                new ShiftCodeCreateInput("OP" + UUID.randomUUID().toString().substring(0, 4), true, true, "2026-01-01").staffArea(StaffArea.MAINTENANCE),
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
                new ShiftCodeCreateInput(code, true, true, "2026-01-01").startTime1("09:00").endTime1("17:00"),
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
                new ShiftCodeCreateInput(code, true, true, "2026-01-01").startTime1("09:00").endTime1("17:00"),
                manager.getId());
        createdShiftCodeIds.add(shared.getId());

        ShiftCode restaurantOverride = shiftCodeService.create(
                new ShiftCodeCreateInput(code, true, true, "2026-01-01").staffArea(StaffArea.RESTAURANT).startTime1("09:00").endTime1("13:00").startTime2("16:00").endTime2("21:00"),
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
}
