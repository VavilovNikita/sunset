package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.StaffAreaCoverageRule;
import com.sunsetbeach.model.StaffAreaCoverageRuleInput;
import com.sunsetbeach.repository.StaffAreaCoverageRuleRepository;
import com.sunsetbeach.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;

/** An area with no rule set has no minimum and never warns - see RosterServiceTests for the warning itself. */
@SpringBootTest
class StaffAreaCoverageRuleServiceTests extends AbstractIntegrationTest {

    @Autowired
    private StaffAreaCoverageRuleService staffAreaCoverageRuleService;

    @Autowired
    private StaffAreaCoverageRuleRepository staffAreaCoverageRuleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final List<String> createdUserIds = new ArrayList<>();

    @AfterEach
    void cleanUp() {
        staffAreaCoverageRuleRepository.deleteAll();
        createdUserIds.forEach(userRepository::deleteById);
    }

    private UserEntity createManager() {
        UserEntity user = new UserEntity();
        user.setEmail("coverage-rule-test-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash(passwordEncoder.encode("irrelevant1"));
        user.setRole(Role.MANAGER);
        user.setActive(true);
        UserEntity saved = userRepository.saveAndFlush(user);
        createdUserIds.add(saved.getId());
        return saved;
    }

    @Test
    void set_thenList_roundTrips() {
        UserEntity manager = createManager();

        staffAreaCoverageRuleService.set(StaffArea.KITCHEN, new StaffAreaCoverageRuleInput(2), manager.getId());
        List<StaffAreaCoverageRule> rules = staffAreaCoverageRuleService.list();

        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getStaffArea()).isEqualTo(StaffArea.KITCHEN);
        assertThat(rules.get(0).getMinimumWorking()).isEqualTo(2);
        assertThat(rules.get(0).getUpdatedByEmail()).isEqualTo(manager.getEmail());
    }

    @Test
    void set_calledAgain_replacesRatherThanDuplicates() {
        UserEntity manager = createManager();
        staffAreaCoverageRuleService.set(StaffArea.KITCHEN, new StaffAreaCoverageRuleInput(2), manager.getId());

        staffAreaCoverageRuleService.set(StaffArea.KITCHEN, new StaffAreaCoverageRuleInput(3), manager.getId());

        List<StaffAreaCoverageRule> rules = staffAreaCoverageRuleService.list();
        assertThat(rules).hasSize(1);
        assertThat(rules.get(0).getMinimumWorking()).isEqualTo(3);
    }
}
