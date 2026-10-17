package com.sunsetbeach.service;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.AttendanceDeviceEntity;
import com.sunsetbeach.entity.NotificationSettingsEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.StaffChannelLinkEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.AttendancePunchCreateInput;
import com.sunsetbeach.model.PunchDirection;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RosterEntryCreateInput;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.ShiftCodeCreateInput;
import com.sunsetbeach.model.ShiftCodeKind;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.notification.TelegramClient;
import com.sunsetbeach.repository.*;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.support.FakeTelegramClient;
import com.sunsetbeach.support.MutableClock;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Shared fixture for the notification tests. One class so they all share one Spring context (every
 * distinct set of properties/beans is its own context with its own connection pool): a clock the
 * test moves, a Telegram that never touches the network, and every background interval of the
 * notification centre set to an hour so only an explicit {@code runOnce()} ever runs.
 *
 * <p>Not {@code @Transactional}: the services under test commit in their own transactions (and the
 * races need real ones), so everything a test creates is removed in {@link #cleanUp()}. The hotel
 * clock sits on 2033-05-17 so nothing another test class left behind can look "today".
 */
@SpringBootTest(properties = {
    "app.notifications.scan-interval-ms=3600000",
    "app.notifications.delivery-interval-ms=3600000",
    "app.notifications.telegram.poll-interval-ms=3600000",
    "app.notifications.initial-delay-ms=3600000",
    "app.attendance.device-poll-interval-ms=3600000",
    "app.notifications.max-attempts=3",
    // This context is one more in a suite that already sits near the connection ceiling (see AbstractIntegrationTest);
    // its tests are single-threaded apart from a handful of races, so a small pool is plenty.
    "spring.datasource.hikari.maximum-pool-size=8",
    "spring.datasource.hikari.minimum-idle=1"
})
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
@Import(NotificationTestBase.NotificationTestConfig.class)
abstract class NotificationTestBase extends AbstractIntegrationTest {

    static final String DAY = "2033-05-17";
    static final LocalDate TODAY = LocalDate.parse(DAY);

    @TestConfiguration
    static class NotificationTestConfig {
        @Bean
        @Primary
        MutableClock notificationTestClock() {
            return new MutableClock(DAY + "T10:00");
        }

        @Bean
        @Primary
        FakeTelegramClient fakeTelegramClient() {
            return new FakeTelegramClient();
        }
    }

    @Autowired protected MutableClock clock;
    @Autowired protected FakeTelegramClient telegram;
    @Autowired protected NotificationService notificationService;
    @Autowired protected NotificationSettingsService settingsService;
    @Autowired protected NotificationScanService scanService;
    @Autowired protected NotificationDeliveryService deliveryService;
    @Autowired protected NotificationChannelService channelService;
    @Autowired protected TelegramPollService pollService;
    @Autowired protected NotificationLogService logService;
    @Autowired protected NotificationSettingsRepository settingsRepository;
    @Autowired protected NotificationRepository notificationRepository;
    @Autowired protected NotificationDeliveryRepository deliveryRepository;
    @Autowired protected StaffChannelLinkRepository linkRepository;
    @Autowired protected ChannelLinkCodeRepository codeRepository;
    @Autowired protected TerminalSilenceEpisodeRepository episodeRepository;
    @Autowired protected TelegramBotStateRepository botStateRepository;
    @Autowired protected AttendanceDeviceRepository deviceRepository;
    @Autowired protected AttendancePunchRepository punchRepository;
    @Autowired protected RosterEntryRepository rosterEntryRepository;
    @Autowired protected ShiftCodeRepository shiftCodeRepository;
    @Autowired protected EmployeePatternRepository employeePatternRepository;
    @Autowired protected UserRepository userRepository;
    @Autowired protected RoomRepository roomRepository;
    @Autowired protected RoomUnitRepository roomUnitRepository;
    @Autowired protected RoomUnitBlockRepository blockRepository;
    @Autowired protected MaintenanceTaskRepository maintenanceTaskRepository;
    @Autowired protected AuditLogRepository auditLogRepository;
    @Autowired protected RosterService rosterService;
    @Autowired protected ShiftCodeService shiftCodeService;
    @Autowired protected AttendanceService attendanceService;
    @Autowired protected RoomUnitService roomUnitService;
    @Autowired protected MaintenanceTaskService maintenanceTaskService;
    @Autowired protected TelegramClient telegramClient;

    protected final List<String> userIds = new ArrayList<>();
    protected final List<String> shiftCodeIds = new ArrayList<>();
    protected final List<String> deviceIds = new ArrayList<>();
    protected final List<String> blockIds = new ArrayList<>();
    protected final List<String> unitIds = new ArrayList<>();
    protected final List<String> roomIds = new ArrayList<>();
    private final List<String> otherActiveDeviceIds = new ArrayList<>();

    @BeforeEach
    void notificationSetUp() {
        clock.set(DAY + "T10:00");
        telegram.reset();
        resetSettings();
        // Other test classes may leave active terminals behind; with a 2033 clock they would all be
        // "silent". Set aside every device that is not ours for the length of the test.
        for (AttendanceDeviceEntity d : deviceRepository.findAll()) {
            if (d.isActive()) {
                otherActiveDeviceIds.add(d.getId());
                d.setActive(false);
                deviceRepository.saveAndFlush(d);
            }
        }
        episodeRepository.deleteAll();
        // The poll offset and the cached bot name live in the database; one test's must not leak into the next.
        var state = botStateRepository.findById(1).orElseThrow();
        state.setUpdateOffset(0);
        state.setBotUsername(null);
        botStateRepository.saveAndFlush(state);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        deviceRepository.findAllById(deviceIds).forEach(d -> {
            d.setActive(false);
            deviceRepository.saveAndFlush(d);
        });
        episodeRepository.deleteAll();
        deliveryRepository.deleteAll(deliveryRepository.findAll().stream().filter(d -> notificationRecipient(d.getNotificationId())).toList());
        notificationRepository.deleteAll(notificationRepository.findAll().stream().filter(n -> userIds.contains(n.getRecipientUserId())).toList());
        linkRepository.deleteAll(linkRepository.findByUserIdIn(userIds));
        codeRepository.deleteAll(codeRepository.findAll().stream().filter(c -> userIds.contains(c.getUserId())).toList());
        punchRepository.deleteAll(punchRepository.findAll().stream().filter(p -> userIds.contains(p.getEmployeeUserId())).toList());
        rosterEntryRepository.deleteAll(rosterEntryRepository.findAll().stream().filter(e -> userIds.contains(e.getEmployeeUserId())).toList());
        maintenanceTaskRepository.deleteAll(maintenanceTaskRepository.findAll().stream().filter(t -> unitIds.contains(t.getRoomUnitId())).toList());
        blockRepository.deleteAll(blockRepository.findAll().stream().filter(b -> unitIds.contains(b.getRoomUnitId())).toList());
        roomUnitRepository.deleteAllById(unitIds);
        roomRepository.deleteAllById(roomIds);
        userIds.forEach(employeePatternRepository::deleteById);
        shiftCodeRepository.deleteAllById(shiftCodeIds);
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream().filter(a -> a.getEntityId() != null && userIds.contains(a.getEntityId())).toList());
        deviceRepository.deleteAllById(deviceIds);
        userRepository.deleteAllById(userIds);
        for (String id : otherActiveDeviceIds) {
            deviceRepository.findById(id).ifPresent(d -> {
                d.setActive(true);
                deviceRepository.saveAndFlush(d);
            });
        }
        otherActiveDeviceIds.clear();
        resetSettings();
        userIds.clear();
        shiftCodeIds.clear();
        deviceIds.clear();
        blockIds.clear();
        unitIds.clear();
        roomIds.clear();
    }

    private boolean notificationRecipient(String notificationId) {
        return notificationRepository.findById(notificationId).map(n -> userIds.contains(n.getRecipientUserId())).orElse(false);
    }

    /** The seeded defaults (V148), recipients empty. */
    protected void resetSettings() {
        NotificationSettingsEntity s = settingsRepository.findById(1).orElseThrow();
        s.setLateEnabled(true);
        s.setLateGraceMinutes(10);
        s.setAbsenceEnabled(true);
        s.setAbsenceMinutes(60);
        s.setBlockCreatedEnabled(true);
        s.setBlockLastDayEnabled(true);
        s.setBlockExpiredEnabled(true);
        s.setTerminalAlertsEnabled(true);
        s.setTerminalSilenceMinutes(15);
        s.setQuietHoursEnabled(true);
        s.setQuietStart(java.time.LocalTime.of(22, 0));
        s.setQuietEnd(java.time.LocalTime.of(7, 0));
        s.setMorningTime(java.time.LocalTime.of(9, 0));
        s.setDefaultLanguage("EN");
        s.setLateRecipientUserIds(new String[0]);
        s.setDirectorUserIds(new String[0]);
        settingsRepository.saveAndFlush(s);
    }

    protected void settings(java.util.function.Consumer<NotificationSettingsEntity> change) {
        NotificationSettingsEntity s = settingsRepository.findById(1).orElseThrow();
        change.accept(s);
        settingsRepository.saveAndFlush(s);
    }

    protected UserEntity user(String name, Role role, String... functions) {
        UserEntity u = new UserEntity();
        u.setEmail("notif-test-" + UUID.randomUUID() + "@example.com");
        u.setPasswordHash("not-a-real-hash"); // email and password travel together (V68)
        u.setName(name);
        u.setRole(role);
        u.setActive(true);
        u.setJobFunctions(functions);
        UserEntity saved = userRepository.saveAndFlush(u);
        userIds.add(saved.getId());
        return saved;
    }

    /** Links a (fake) Telegram chat to a user directly - the link flow itself has its own tests. */
    protected String link(UserEntity user) {
        String chat = "chat-" + UUID.randomUUID();
        StaffChannelLinkEntity link = new StaffChannelLinkEntity();
        link.setUserId(user.getId());
        link.setChannel("TELEGRAM");
        link.setExternalId(chat);
        linkRepository.saveAndFlush(link);
        return chat;
    }

    protected AttendanceDeviceEntity device(String name, LocalDateTime lastSeenAtUtc) {
        AttendanceDeviceEntity d = new AttendanceDeviceEntity();
        d.setName(name);
        d.setSerial("SN-" + UUID.randomUUID());
        d.setAddress("10.255.255.1");
        d.setTimezone("Asia/Bangkok");
        d.setActive(true);
        d.setLastSeenAt(lastSeenAtUtc);
        AttendanceDeviceEntity saved = deviceRepository.saveAndFlush(d);
        deviceIds.add(saved.getId());
        return saved;
    }

    /** The terminal answered a minute before the (hotel-clock) moment {@code hotelLocal}. */
    protected AttendanceDeviceEntity healthyDevice(String name) {
        return device(name, LocalDateTime.parse(DAY + "T10:00").atZone(MutableClock.HOTEL_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime().minusMinutes(1));
    }

    protected void touchDevice(AttendanceDeviceEntity device, String hotelLocal) {
        AttendanceDeviceEntity d = deviceRepository.findById(device.getId()).orElseThrow();
        d.setLastSeenAt(LocalDateTime.parse(hotelLocal).atZone(MutableClock.HOTEL_ZONE).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime());
        deviceRepository.saveAndFlush(d);
    }

    protected ShiftCode shiftCode(String start, String end, UserEntity actor) {
        ShiftCode code = shiftCodeService.create(
                new ShiftCodeCreateInput("N" + UUID.randomUUID().toString().substring(0, 6), ShiftCodeKind.MORNING, true, true, "2020-01-01")
                        .staffArea(StaffArea.RESTAURANT)
                        .startTime1(start)
                        .endTime1(end),
                actor.getId());
        shiftCodeIds.add(code.getId());
        return code;
    }

    protected void roster(UserEntity employee, ShiftCode code, UserEntity actor) {
        rosterService.createEntry(new RosterEntryCreateInput(employee.getId(), DAY, code.getId()), actor.getId());
    }

    /** A manual punch at a hotel time today (the offset is the hotel's +07:00). */
    protected void punchIn(UserEntity employee, String time, UserEntity actor) {
        attendanceService.recordPunch(
                new AttendancePunchCreateInput(employee.getId(), java.time.OffsetDateTime.parse(DAY + "T" + time + ":00+07:00"), PunchDirection.IN), actor.getId());
    }

    protected RoomUnitEntity roomUnit() {
        RoomEntity room = new RoomEntity();
        room.setName("Notif Test Room " + UUID.randomUUID());
        room.setDescription("Notification tests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        roomIds.add(savedRoom.getId());
        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("N" + UUID.randomUUID().toString().substring(0, 5));
        unit.setActive(true);
        RoomUnitEntity saved = roomUnitRepository.saveAndFlush(unit);
        unitIds.add(saved.getId());
        return saved;
    }

    protected void actAs(UserEntity user) {
        StaffPrincipal principal = new StaffPrincipal(user.getId(), user.getEmail(), user.getRole());
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(principal, null, List.of()));
    }

    /** Notifications written to one user, oldest first. */
    protected List<com.sunsetbeach.entity.NotificationEntity> notificationsOf(UserEntity user) {
        return notificationRepository.findAll().stream()
                .filter(n -> n.getRecipientUserId().equals(user.getId()))
                .sorted(java.util.Comparator.comparing(com.sunsetbeach.entity.NotificationEntity::getCreatedAt).thenComparing(com.sunsetbeach.entity.NotificationEntity::getId))
                .toList();
    }

    protected List<com.sunsetbeach.entity.NotificationEntity> notificationsOf(UserEntity user, String type) {
        return notificationsOf(user).stream().filter(n -> n.getType().equals(type)).toList();
    }
}
