package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomUnit;
import com.sunsetbeach.model.RoomUnitBlock;
import com.sunsetbeach.model.RoomUnitBlockInput;
import com.sunsetbeach.model.RoomUnitInput;
import com.sunsetbeach.model.RoomUnitUpdateInput;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserCreateInput;
import com.sunsetbeach.model.UserRoleUpdateInput;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * DB-backed (real dev Postgres), NOT {@code @Transactional} - see {@link BookingAuditLogTests}'s
 * javadoc for why (AuditLogService.record commits independently of this test's transaction).
 */
@SpringBootTest
class UserRoomUnitAuditLogTests extends AbstractIntegrationTest {

    @Autowired
    private UserService userService;

    @Autowired
    private RoomUnitService roomUnitService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    @Autowired
    private RoomUnitBlockRepository roomUnitBlockRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    private final List<String> createdUserIds = new java.util.ArrayList<>();
    private final List<String> createdRoomIds = new java.util.ArrayList<>();
    private final List<String> createdUnitIds = new java.util.ArrayList<>();

    @BeforeEach
    void setUpSecurityContext() {
        StaffPrincipal principal = new StaffPrincipal("admin-actor", "audit-admin-test@example.com", Role.ADMIN);
        var authorities = List.of(new SimpleGrantedAuthority("ROLE_ADMIN"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, authorities));
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String userId : createdUserIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.USER, userId));
            userRepository.deleteById(userId);
        }
        for (String unitId : createdUnitIds) {
            auditLogRepository.deleteAll(entriesFor(AuditEntityType.ROOM_UNIT, unitId));
            roomUnitBlockRepository.deleteAll(roomUnitBlockRepository.findByRoomUnitId(unitId));
            roomUnitRepository.findById(unitId).ifPresent(roomUnitRepository::delete);
        }
        for (String roomId : createdRoomIds) {
            roomRepository.deleteById(roomId);
        }
    }

    private List<AuditLogEntity> entriesFor(AuditEntityType entityType, String entityId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == entityType && entityId.equals(e.getEntityId()))
                .toList();
    }

    private RoomEntity createRoom() {
        RoomEntity room = new RoomEntity();
        room.setName("Audit RoomUnit Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by UserRoomUnitAuditLogTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity saved = roomRepository.saveAndFlush(room);
        createdRoomIds.add(saved.getId());
        return saved;
    }

    @Test
    void createUser_writesUserCreatedEntry() {
        String email = "audit-new-user-" + UUID.randomUUID() + "@example.com";
        UserCreateInput input = new UserCreateInput(email, "password1234");
        input.setRole(Role.CASHIER);

        User created = userService.create(input);
        createdUserIds.add(created.getId());

        List<AuditLogEntity> entries = entriesFor(AuditEntityType.USER, created.getId());
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getAction()).isEqualTo(AuditAction.USER_CREATED);
        assertThat(entries.get(0).getSummary()).contains(email).contains("CASHIER");
    }

    @Test
    void updateRole_writesUserRoleChangedEntry() {
        UserCreateInput input = new UserCreateInput("audit-role-user-" + UUID.randomUUID() + "@example.com", "password1234");
        input.setRole(Role.WAITER);
        User created = userService.create(input);
        createdUserIds.add(created.getId());

        userService.updateRole(created.getId(), "admin-actor", new UserRoleUpdateInput(Role.CASHIER));

        List<AuditLogEntity> entries = entriesFor(AuditEntityType.USER, created.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.USER_ROLE_CHANGED)
                .toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).contains("WAITER").contains("CASHIER");
    }

    @Test
    void setActive_writesUserActiveChangedEntry() {
        UserCreateInput input = new UserCreateInput("audit-active-user-" + UUID.randomUUID() + "@example.com", "password1234");
        User created = userService.create(input);
        createdUserIds.add(created.getId());

        userService.setActive(created.getId(), "admin-actor", false);

        List<AuditLogEntity> entries = entriesFor(AuditEntityType.USER, created.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.USER_ACTIVE_CHANGED)
                .toList();
        assertThat(entries).hasSize(1);
        assertThat(entries.get(0).getSummary()).contains("disabled");
    }

    @Test
    void resetPassword_writesUserPasswordResetEntry() {
        UserCreateInput input = new UserCreateInput("audit-reset-user-" + UUID.randomUUID() + "@example.com", "password1234");
        User created = userService.create(input);
        createdUserIds.add(created.getId());

        userService.resetPassword(created.getId(), "a-brand-new-password1");

        List<AuditLogEntity> entries = entriesFor(AuditEntityType.USER, created.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.USER_PASSWORD_RESET)
                .toList();
        assertThat(entries).hasSize(1);
    }

    @Test
    void createUpdateDeleteRoomUnit_writeExpectedEntries() {
        RoomEntity room = createRoom();

        RoomUnit unit = roomUnitService.create(new RoomUnitInput(room.getId(), "Audit Unit " + UUID.randomUUID()));
        createdUnitIds.add(unit.getId());

        List<AuditLogEntity> createdEntries = entriesFor(AuditEntityType.ROOM_UNIT, unit.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.ROOM_UNIT_CREATED)
                .toList();
        assertThat(createdEntries).hasSize(1);

        String newLabel = "Renamed Unit " + UUID.randomUUID();
        roomUnitService.update(unit.getId(), new RoomUnitUpdateInput(newLabel, true));

        List<AuditLogEntity> updatedEntries = entriesFor(AuditEntityType.ROOM_UNIT, unit.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.ROOM_UNIT_UPDATED)
                .toList();
        assertThat(updatedEntries).hasSize(1);
        assertThat(updatedEntries.get(0).getSummary()).contains("renamed");

        String unitId = unit.getId();
        roomUnitService.delete(unitId);
        createdUnitIds.remove(unitId);

        List<AuditLogEntity> deletedEntries = entriesFor(AuditEntityType.ROOM_UNIT, unitId).stream()
                .filter(e -> e.getAction() == AuditAction.ROOM_UNIT_DELETED)
                .toList();
        assertThat(deletedEntries).hasSize(1);
        auditLogRepository.deleteAll(entriesFor(AuditEntityType.ROOM_UNIT, unitId));
    }

    @Test
    void createAndDeleteBlock_writeExpectedEntries() {
        RoomEntity room = createRoom();
        RoomUnit unit = roomUnitService.create(new RoomUnitInput(room.getId(), "Audit Block Unit " + UUID.randomUUID()));
        createdUnitIds.add(unit.getId());

        LocalDate from = LocalDate.now().plusDays(20);
        LocalDate to = LocalDate.now().plusDays(22);
        RoomUnitBlock block = roomUnitService.createBlock(unit.getId(), new RoomUnitBlockInput(from.toString(), to.toString(), "Maintenance"));

        List<AuditLogEntity> createEntries = entriesFor(AuditEntityType.ROOM_UNIT, unit.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.ROOM_UNIT_BLOCK_CREATED)
                .toList();
        assertThat(createEntries).hasSize(1);
        assertThat(createEntries.get(0).getSummary()).contains("Maintenance").contains(from.toString());

        roomUnitService.deleteBlock(unit.getId(), block.getId());

        List<AuditLogEntity> deleteEntries = entriesFor(AuditEntityType.ROOM_UNIT, unit.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.ROOM_UNIT_BLOCK_DELETED)
                .toList();
        assertThat(deleteEntries).hasSize(1);
    }
}
