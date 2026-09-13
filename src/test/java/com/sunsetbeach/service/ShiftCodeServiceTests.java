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
                new ShiftCodeCreateInput(StaffArea.RESTAURANT, code, true, true, "2026-01-01").startTime1("09:00").endTime1("17:00"),
                manager.getId());
        createdShiftCodeIds.add(first.getId());
        assertThat(first.getActive()).isTrue();

        ShiftCode second = shiftCodeService.create(
                new ShiftCodeCreateInput(StaffArea.RESTAURANT, code, true, true, "2026-02-01").startTime1("10:00").endTime1("18:00"),
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
                new ShiftCodeCreateInput(StaffArea.RESTAURANT, code, true, true, "2026-01-01").startTime1("09:00").endTime1("13:00").startTime2("16:00").endTime2("21:00"),
                manager.getId());
        createdShiftCodeIds.add(restaurantVersion.getId());
        ShiftCode frontOfficeVersion = shiftCodeService.create(
                new ShiftCodeCreateInput(StaffArea.FRONT_OFFICE, code, true, true, "2026-01-01").startTime1("09:00").endTime1("18:00"),
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
                        new ShiftCodeCreateInput(StaffArea.KITCHEN, "BAD" + UUID.randomUUID().toString().substring(0, 4), true, true, "2026-01-01")
                                .startTime2("18:00")
                                .endTime2("21:00"),
                        manager.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_endBeforeStart_isRejected() {
        UserEntity manager = createManager();
        assertThatThrownBy(() -> shiftCodeService.create(
                        new ShiftCodeCreateInput(StaffArea.KITCHEN, "BAD" + UUID.randomUUID().toString().substring(0, 4), true, true, "2026-01-01")
                                .startTime1("18:00")
                                .endTime1("09:00"),
                        manager.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void create_zeroIntervals_isOpAndCountsAsWorked() {
        UserEntity manager = createManager();
        ShiftCode op = shiftCodeService.create(
                new ShiftCodeCreateInput(StaffArea.MAINTENANCE, "OP" + UUID.randomUUID().toString().substring(0, 4), true, true, "2026-01-01"),
                manager.getId());
        createdShiftCodeIds.add(op.getId());

        assertThat(op.getCountsAsWorked()).isTrue();
        assertThat(op.getStartTime1().isPresent()).isFalse();
        assertThat(op.getStartTime2().isPresent()).isFalse();
    }
}
