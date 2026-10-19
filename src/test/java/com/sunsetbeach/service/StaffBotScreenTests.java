package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sunsetbeach.bot.StaffBotService;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.ShiftChangeRequestEntity;
import com.sunsetbeach.entity.StaffActionEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BlockResponsibleFunction;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.MaintenanceTask;
import com.sunsetbeach.model.MaintenanceTaskStatus;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomUnitBlockInput;
import com.sunsetbeach.model.ShiftChangeRequestChannel;
import com.sunsetbeach.model.ShiftChangeRequestType;
import com.sunsetbeach.model.ShiftCode;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.SpaAppointmentStatusUpdateInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.TelegramClient.TelegramUpdate;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.SpaAppointmentTreatmentRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.support.FakeTelegramClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Part two of the staff bot: the role screens (an engineer's tasks, a housekeeper's blocks, a therapist's procedures,
 * "who is on shift", the managers' summary), the two morning messages and the HR director's own requests. Over the
 * real services and database with a Telegram that never touches the network and the clock the test moves (2033-05-17,
 * hotel time). The tests are the things that would silently give a wrong answer: a close that skips the block, a button
 * that still works after the function was taken away, two closes at once, a guest's name in a therapist's message, a
 * silent terminal turned into "everybody is late", a summary that disagrees with the admin screens, a second summary
 * on the same day, the HR director approving their own shift.
 */
class StaffBotScreenTests extends NotificationTestBase {

    private static final LocalDate TOMORROW = TODAY.plusDays(1);

    @Autowired private StaffBotService botService;
    @Autowired private StaffSummaryService summaryService;
    @Autowired private ShiftChangeRequestService requestService;
    @Autowired private ShiftRequestDeciderService deciderService;
    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private TableRepository tableRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private SpaAppointmentService spaService;
    @Autowired private SpaAppointmentRepository spaAppointmentRepository;
    @Autowired private SpaAppointmentTreatmentRepository spaTreatmentRepository;
    @Autowired private org.springframework.jdbc.core.JdbcTemplate jdbc;
    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<String> tableIds = new ArrayList<>();
    private final List<String> menuItemIds = new ArrayList<>();
    private long nextUpdate = 1;

    private UserEntity manager;
    private String managerChat;

    @BeforeEach
    void people() {
        manager = user("Manager", Role.MANAGER);
        managerChat = link(manager);
    }

    @AfterEach
    void removeWhatTheScreensWrote() {
        // Before the base class removes users, units and rooms: appointments, then their tables and treatments, then bookings.
        var appointments = spaAppointmentRepository.findAll().stream().filter(a -> tableIds.contains(a.getTableId())).toList();
        spaTreatmentRepository.deleteAll(spaTreatmentRepository.findBySpaAppointmentIdIn(appointments.stream().map(a -> a.getId()).toList()));
        spaAppointmentRepository.deleteAll(appointments);
        tableIds.forEach(tableRepository::deleteById);
        menuItemIds.forEach(menuItemRepository::deleteById);
        bookingRepository.deleteAll(bookingRepository.findAll().stream().filter(b -> roomIds.contains(b.getRoomId())).toList());
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream().filter(a -> userIds.contains(a.getActorUserId())).toList());
    }

    // ---- helpers ---------------------------------------------------------------------------------------------

    private void say(String chat, String text) {
        telegram.incoming.add(new TelegramUpdate(nextUpdate++, chat, text));
        pollService.pollOnce();
    }

    private void press(String chat, String callbackData) {
        telegram.incoming.add(new TelegramUpdate(nextUpdate++, chat, null, "cq" + nextUpdate, callbackData, "1"));
        pollService.pollOnce();
    }

    /** A button row made for {@code who} as the bot would have made it earlier (its id is all a press carries). */
    private String button(UserEntity who, String kind, String subjectId) {
        StaffActionEntity a = new StaffActionEntity();
        a.setId(BotActionIds.next());
        a.setUserId(who.getId());
        a.setKind(kind);
        a.setSubjectId(subjectId);
        staffActionRepository.saveAndFlush(a);
        return "a:" + a.getId();
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, UserEntity who) {
        return request.header("Authorization", "Bearer " + jwtService.issue(new StaffPrincipal(who.getId(), who.getEmail(), who.getRole())));
    }

    private UserEntity linkedAdmin(String name) {
        UserEntity admin = user(name, Role.ADMIN);
        link(admin);
        return admin;
    }

    private Booking booking(RoomUnitEntity unit, String guest, LocalDate checkIn, LocalDate checkOut) {
        actAs(manager);
        Booking booking = bookingService.createStaffBooking(
                new StaffBookingCreateInput(unit.getRoomId(), guest, checkIn.toString(), checkOut.toString(), BookingChannel.WALK_IN, 1).roomUnitId(unit.getId()));
        SecurityContextHolderClear.clear();
        return booking;
    }

    private MaintenanceTask taskWithBlock(RoomUnitEntity unit, String description, LocalDate from, LocalDate to) {
        MaintenanceTask task = maintenanceTaskService.create(unit.getId(), description, List.of(), manager.getId());
        blockIds.add(maintenanceTaskService.addBlock(task.getId(), new RoomUnitBlockInput(from.toString(), to.toString(), description), manager.getId()).getTask().getBlockId().get());
        return task;
    }

    // ---- the engineer's tasks --------------------------------------------------------------------------------

    @Test
    void done_fromTheBot_closesTheTaskAndShortensTheBlockLikeTheAdminScreen_andAuditsThePresser() {
        UserEntity engineer = user("Eddy Engineer", Role.WAITER, "ENGINEER");
        String chat = link(engineer);
        RoomUnitEntity botUnit = roomUnit();
        RoomUnitEntity adminUnit = roomUnit();
        MaintenanceTask viaBot = taskWithBlock(botUnit, "Leaking tap", TODAY.minusDays(2), TODAY.plusDays(4));
        MaintenanceTask viaAdmin = taskWithBlock(adminUnit, "Leaking tap", TODAY.minusDays(2), TODAY.plusDays(4));

        // The admin screen's path.
        actAs(manager);
        maintenanceTaskService.updateStatus(viaAdmin.getId(), MaintenanceTaskStatus.DONE);
        SecurityContextHolderClear.clear();
        // The bot's: a button pressed from the engineer's own chat.
        press(chat, button(engineer, com.sunsetbeach.notification.NotificationAction.Kinds.TASK_DONE, viaBot.getId()));

        var botTask = maintenanceTaskRepository.findById(viaBot.getId()).orElseThrow();
        var adminTask = maintenanceTaskRepository.findById(viaAdmin.getId()).orElseThrow();
        assertThat(botTask.getStatus()).isEqualTo(MaintenanceTaskStatus.DONE);
        assertThat(botTask.getClosedAt()).isNotNull();
        RoomUnitBlockEntity botBlock = blockRepository.findById(botTask.getBlockId()).orElseThrow();
        RoomUnitBlockEntity adminBlock = blockRepository.findById(adminTask.getBlockId()).orElseThrow();
        // Shortened to yesterday - the same end the admin screen's close gives, and never extended.
        assertThat(botBlock.getToDate()).isEqualTo(TODAY.minusDays(1)).isEqualTo(adminBlock.getToDate());
        // Audit: the engineer, "via Telegram", for the status change and for the block that was shortened.
        var mine = auditLogRepository.findAll().stream().filter(a -> engineer.getId().equals(a.getActorUserId())).toList();
        assertThat(mine).extracting(a -> a.getAction()).containsExactlyInAnyOrder(AuditAction.MAINTENANCE_TASK_STATUS_CHANGED, AuditAction.MAINTENANCE_TASK_BLOCK_LIFTED);
        assertThat(mine).allSatisfy(a -> assertThat(a.getSummary()).endsWith("(via Telegram)"));
        assertThat(telegram.answers).last().isEqualTo("Done.");
    }

    @Test
    void startAndDone_areForwardOnly_andARepeatedPressAnswersWithTheCurrentStatus() {
        UserEntity engineer = user("Eddy Engineer", Role.WAITER, "ENGINEER");
        String chat = link(engineer);
        MaintenanceTask task = maintenanceTaskService.create(roomUnit().getId(), "Flickering light", List.of(), manager.getId());

        press(chat, button(engineer, "TASK_START", task.getId()));
        assertThat(maintenanceTaskRepository.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(MaintenanceTaskStatus.IN_PROGRESS);
        assertThat(telegram.answers).last().isEqualTo("Started.");

        press(chat, button(engineer, "TASK_START", task.getId()));
        assertThat(telegram.answers).last().isEqualTo("Already in progress.");

        press(chat, button(engineer, "TASK_DONE", task.getId()));
        press(chat, button(engineer, "TASK_DONE", task.getId()));
        press(chat, button(engineer, "TASK_START", task.getId()));
        assertThat(maintenanceTaskRepository.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(MaintenanceTaskStatus.DONE);
        assertThat(telegram.answers).last().isEqualTo("Already done.");
        // One status change per real move: start, done - the repeats wrote nothing.
        assertThat(auditLogRepository.findAll().stream()
                .filter(a -> engineer.getId().equals(a.getActorUserId()) && a.getAction() == AuditAction.MAINTENANCE_TASK_STATUS_CHANGED)).hasSize(2);
    }

    @Test
    void anEngineerWhoLostTheFunctionAfterTheMessageWasSent_pressingDoneChangesNothing() {
        UserEntity engineer = user("Eddy Engineer", Role.WAITER, "ENGINEER");
        String chat = link(engineer);
        MaintenanceTask task = maintenanceTaskService.create(roomUnit().getId(), "Broken lock", List.of(), manager.getId());
        String done = button(engineer, "TASK_DONE", task.getId());

        engineer.setJobFunctions(new String[0]);
        userRepository.saveAndFlush(engineer);
        press(chat, done);

        assertThat(maintenanceTaskRepository.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(MaintenanceTaskStatus.OPEN);
        assertThat(telegram.answers).last().isEqualTo(NotificationMessages.render(NotificationMessages.Key.BOT_NOT_ALLOWED, com.sunsetbeach.model.NotificationLanguage.EN, java.util.Map.of()));
        say(chat, "/tasks");
        assertThat(telegram.lastTo(chat).text()).isEqualTo("You can't do that.");

        // The same button in the hands of the switch: turned off by the administrator, it is refused too.
        engineer.setJobFunctions(new String[] {"ENGINEER"});
        userRepository.saveAndFlush(engineer);
        botSettings(s -> s.setEngineerTasksEnabled(false));
        press(chat, button(engineer, "TASK_DONE", task.getId()));
        assertThat(maintenanceTaskRepository.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(MaintenanceTaskStatus.OPEN);
    }

    @Test
    void twoDoneAtOnce_closeTheTaskOnce() throws Exception {
        UserEntity first = user("Engineer One", Role.WAITER, "ENGINEER");
        UserEntity second = user("Engineer Two", Role.WAITER, "ENGINEER");
        String chat1 = link(first);
        String chat2 = link(second);
        MaintenanceTask task = taskWithBlock(roomUnit(), "Dead socket", TODAY.minusDays(1), TODAY.plusDays(3));
        String done1 = button(first, "TASK_DONE", task.getId());
        String done2 = button(second, "TASK_DONE", task.getId());

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures = new ArrayList<>();
            futures.add(pool.submit(() -> {
                barrier.await();
                botService.handle(new TelegramUpdate(901, chat1, null, "cq-a", done1, "1"));
                return null;
            }));
            futures.add(pool.submit(() -> {
                barrier.await();
                botService.handle(new TelegramUpdate(902, chat2, null, "cq-b", done2, "1"));
                return null;
            }));
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(maintenanceTaskRepository.findById(task.getId()).orElseThrow().getStatus()).isEqualTo(MaintenanceTaskStatus.DONE);
        // One of them closed it; the other was told it was already done. One status change, one block shortening.
        assertThat(telegram.answers).containsExactlyInAnyOrder("Done.", "Already done.");
        var rows = auditLogRepository.findAll().stream().filter(a -> userIds.contains(a.getActorUserId())).toList();
        assertThat(rows.stream().filter(a -> a.getAction() == AuditAction.MAINTENANCE_TASK_STATUS_CHANGED)).hasSize(1);
        assertThat(rows.stream().filter(a -> a.getAction() == AuditAction.MAINTENANCE_TASK_BLOCK_LIFTED)).hasSize(1);
    }

    @Test
    void theTasksList_showsRoomAgeAndBlock_withStartAndDoneButtons() {
        UserEntity engineer = user("Eddy Engineer", Role.WAITER, "ENGINEER");
        String chat = link(engineer);
        RoomUnitEntity unit = roomUnit();
        MaintenanceTask task = taskWithBlock(unit, "Cracked tile", TODAY, TODAY.plusDays(5));
        // createdAt is stamped from the real clock; put it where the test clock is (3 days ago, 10:00 hotel time).
        jdbc.update("UPDATE \"MaintenanceTask\" SET \"createdAt\" = ? WHERE id = ?",
                java.sql.Timestamp.valueOf(java.time.LocalDateTime.parse(TODAY.minusDays(3) + "T03:00")), task.getId());

        say(chat, "/tasks");

        // Other test classes may leave open tasks behind and the list is capped; look for ours among what was sent.
        var message = telegram.sent.stream().filter(s -> s.chatId().equals(chat) && s.text().contains(unit.getLabel())).findFirst();
        org.junit.jupiter.api.Assumptions.assumeTrue(message.isPresent(), "list capped by tasks left over from other classes");
        assertThat(message.get().text()).contains("Cracked tile").contains("Open 3 d").contains("Room blocked until " + TODAY.plusDays(5));
        assertThat(FakeTelegramClient.callbackOf(message.get(), "Start")).isNotNull();
        assertThat(FakeTelegramClient.callbackOf(message.get(), "Done")).isNotNull();
    }

    // ---- who sees what -----------------------------------------------------------------------------------------

    @Test
    void roleScreens_areInTheMenuAndTheSlashListOnlyOfWhoMayUseThem() {
        UserEntity waiter = user("Wanda Waiter", Role.WAITER);
        UserEntity engineer = user("Eddy Engineer", Role.WAITER, "ENGINEER");
        UserEntity housekeeper = user("Hilda Housekeeper", Role.WAITER, "HOUSEKEEPER");
        UserEntity therapist = user("Tina Therapist", Role.WAITER, "THERAPIST");
        UserEntity hrDirector = user("Hana HR", Role.WAITER, "HR");
        String waiterChat = link(waiter);
        String engineerChat = link(engineer);
        String housekeeperChat = link(housekeeper);
        String therapistChat = link(therapist);
        String hrChat = link(hrDirector);

        // Before anybody opens the menu everyone sees the same short list: the menu plus what is for everyone.
        // The default list is set once per run; set it again for this test.
        org.springframework.test.util.ReflectionTestUtils.setField(botService, "defaultCommandsSet", new java.util.concurrent.atomic.AtomicBoolean(false));
        botService.ensureDefaultCommands();
        assertThat(telegram.commandSets.stream().filter(c -> c.startsWith("default:"))).isNotEmpty().allSatisfy(c -> assertThat(c).endsWith(":6"));

        for (String chat : List.of(waiterChat, engineerChat, housekeeperChat, therapistChat, hrChat, managerChat)) {
            say(chat, "/menu");
        }
        // The person's own list: 6 for everyone + the screens of their role/function.
        assertThat(commandsOf(waiterChat)).isEqualTo(6);
        assertThat(commandsOf(engineerChat)).isEqualTo(7); // + tasks
        assertThat(commandsOf(housekeeperChat)).isEqualTo(7); // + blocks
        assertThat(commandsOf(therapistChat)).isEqualTo(7); // + procedures
        assertThat(commandsOf(hrChat)).isEqualTo(9); // + pending, vacation, who is on shift (allowed by default for HR)
        assertThat(commandsOf(managerChat)).isEqualTo(9); // + tasks (manager fallback), who is on shift, summary

        // A screen a person may not use is refused when typed; one the administrator switched off says so.
        say(waiterChat, "/summary");
        assertThat(telegram.lastTo(waiterChat).text()).isEqualTo("You can't do that.");
        say(engineerChat, "/onshift");
        assertThat(telegram.lastTo(engineerChat).text()).isEqualTo("You can't do that.");
        say(therapistChat, "/blocks");
        assertThat(telegram.lastTo(therapistChat).text()).isEqualTo("You can't do that.");

        botSettings(s -> s.setWhoIsOnShiftForHr(false));
        say(hrChat, "/menu");
        assertThat(commandsOf(hrChat)).isEqualTo(8);
        say(hrChat, "/onshift");
        assertThat(telegram.lastTo(hrChat).text()).isEqualTo("You can't do that.");

        botSettings(s -> s.setSummaryCommandEnabled(false));
        say(managerChat, "/summary");
        assertThat(telegram.lastTo(managerChat).text()).isEqualTo("This function is turned off.");
    }

    /** The size of the newest per-chat "/" list set for a chat. */
    private int commandsOf(String chat) {
        String last = telegram.commandSets.stream().filter(c -> c.startsWith(chat + ":")).reduce((a, b) -> b).orElseThrow();
        return Integer.parseInt(last.substring(last.lastIndexOf(':') + 1));
    }

    // ---- the housekeeper's blocks --------------------------------------------------------------------------------

    @Test
    void theHousekeepersBlocks_areOnlyTheirFunctions_withinTheHorizon_andReadOnly() {
        UserEntity housekeeper = user("Hilda Housekeeper", Role.WAITER, "HOUSEKEEPER");
        String chat = link(housekeeper);
        RoomUnitEntity now = roomUnit();
        RoomUnitEntity soon = roomUnit();
        RoomUnitEntity far = roomUnit();
        RoomUnitEntity engineers = roomUnit();
        actAs(manager);
        blockIds.add(roomUnitService.createBlock(now.getId(), new RoomUnitBlockInput(TODAY.minusDays(1).toString(), TODAY.plusDays(1).toString(), "Deep clean after flood")
                .responsibleFunction(BlockResponsibleFunction.HOUSEKEEPER), manager.getId()).getBlock().getId());
        blockIds.add(roomUnitService.createBlock(soon.getId(), new RoomUnitBlockInput(TODAY.plusDays(6).toString(), TODAY.plusDays(8).toString(), "Carpet")
                .responsibleFunction(BlockResponsibleFunction.HOUSEKEEPER), manager.getId()).getBlock().getId());
        blockIds.add(roomUnitService.createBlock(far.getId(), new RoomUnitBlockInput(TODAY.plusDays(10).toString(), TODAY.plusDays(11).toString(), "Later")
                .responsibleFunction(BlockResponsibleFunction.HOUSEKEEPER), manager.getId()).getBlock().getId());
        blockIds.add(roomUnitService.createBlock(engineers.getId(), new RoomUnitBlockInput(TODAY.toString(), TODAY.plusDays(2).toString(), "Air conditioner")
                .responsibleFunction(BlockResponsibleFunction.ENGINEER), manager.getId()).getBlock().getId());
        SecurityContextHolderClear.clear();

        say(chat, "/blocks");
        String text = telegram.lastTo(chat).text();
        assertThat(text).contains("Room " + now.getLabel() + ": " + TODAY.minusDays(1) + " - " + TODAY.plusDays(1) + ". Deep clean after flood");
        assertThat(text).contains("Room " + soon.getLabel()).contains("Carpet");
        assertThat(text).doesNotContain(far.getLabel()).doesNotContain(engineers.getLabel());
        assertThat(telegram.lastTo(chat).buttons()).isNullOrEmpty(); // read only

        botSettings(s -> s.setHousekeeperBlocksDays(14));
        say(chat, "/blocks");
        assertThat(telegram.lastTo(chat).text()).contains(far.getLabel());
    }

    // ---- the therapist's procedures --------------------------------------------------------------------------------

    private record SpaFixture(Booking booking, TableEntity table, MenuItemEntity treatment) {
    }

    private SpaFixture spa(String guest, LocalDate from, LocalDate to) {
        TableEntity table = new TableEntity();
        table.setZone(Zone.SPA);
        table.setLabel("Spa T" + UUID.randomUUID().toString().substring(0, 5));
        table.setCapacity(1);
        table.setActive(true);
        table = tableRepository.saveAndFlush(table);
        tableIds.add(table.getId());
        MenuItemEntity item = new MenuItemEntity();
        item.setName("Thai massage " + UUID.randomUUID().toString().substring(0, 5));
        item.setDescription("Screen tests");
        item.setCategory("Massage");
        item.setDepartment(MenuDepartment.SPA);
        item.setPrice(new BigDecimal("1500.00"));
        item.setAvailable(true);
        item.setDurationMinutes(60);
        item = menuItemRepository.saveAndFlush(item);
        menuItemIds.add(item.getId());
        return new SpaFixture(booking(roomUnit(), guest, from, to), table, item);
    }

    private String appointment(SpaFixture f, UserEntity therapist, LocalDate day, String time) {
        return spaService.create(new SpaAppointmentCreateInput(f.booking().getId(), f.table().getId(), therapist.getId(), f.treatment().getId(), day.toString(), time),
                manager.getId()).getAppointment().getId();
    }

    @Test
    void aTherapist_seesOnlyTheirOwnProcedures_withoutTheGuestsName() {
        UserEntity therapist = user("Tina Therapist", Role.WAITER, "THERAPIST");
        UserEntity colleague = user("Carl Colleague", Role.WAITER, "THERAPIST");
        String chat = link(therapist);
        SpaFixture f = spa("Zed Zebra", TODAY, TODAY.plusDays(4));
        String roomLabel = roomUnitRepository.findById(f.booking().getRoomUnitId().get()).orElseThrow().getLabel();
        appointment(f, therapist, TODAY, "11:00");
        appointment(f, colleague, TODAY, "13:00");
        String cancelled = appointment(f, therapist, TODAY, "15:00");
        spaService.updateStatus(cancelled, new SpaAppointmentStatusUpdateInput(SpaAppointmentStatus.CANCELLED), manager.getId());
        appointment(f, therapist, TOMORROW, "12:00");
        appointment(f, therapist, TODAY.plusDays(2), "12:00"); // outside the default two days

        say(chat, "/procedures");

        String text = telegram.lastTo(chat).text();
        assertThat(text).contains("11:00, 60 min: " + f.treatment().getName() + " - table " + f.table().getLabel() + ", room " + roomLabel);
        assertThat(text).contains("12:00");
        assertThat(text).doesNotContain("13:00").doesNotContain("15:00");
        assertThat(text).doesNotContain("Zed").doesNotContain("Zebra");
        assertThat(text.lines().filter(l -> l.contains("min:")).count()).isEqualTo(2);

        // The horizon is a setting.
        botSettings(s -> s.setTherapistScheduleDays(3));
        say(chat, "/procedures");
        assertThat(telegram.lastTo(chat).text().lines().filter(l -> l.contains("min:")).count()).isEqualTo(3);
        // A colleague's day is theirs alone.
        String colleagueChat = link(colleague);
        say(colleagueChat, "/procedures");
        assertThat(telegram.lastTo(colleagueChat).text()).contains("13:00").doesNotContain("11:00");
    }

    @Test
    void theTherapistsMorningMessage_isTheirOwnChoice_oncePerDay_andOnlyWhenThereIsSomethingToDo() {
        UserEntity opted = user("Tina Therapist", Role.WAITER, "THERAPIST");
        UserEntity notOpted = user("Carl Colleague", Role.WAITER, "THERAPIST");
        UserEntity idle = user("Ida Idle", Role.WAITER, "THERAPIST");
        String optedChat = link(opted);
        SpaFixture f = spa("Guest", TODAY, TODAY.plusDays(2));
        appointment(f, opted, TODAY, "14:00");
        appointment(f, opted, TODAY, "11:30");
        appointment(f, notOpted, TODAY, "16:00");

        // The therapist switches it on in the bot's Settings: audited as them, an unchanged press writes nothing.
        say(optedChat, "/settings");
        String on = FakeTelegramClient.callbackOf(telegram.lastTo(optedChat), "Turn the morning message on");
        assertThat(on).isNotNull();
        press(optedChat, on);
        press(optedChat, on);
        idle.setTherapistMorningEnabled(true);
        userRepository.saveAndFlush(idle);
        assertThat(userRepository.findById(opted.getId()).orElseThrow().getTherapistMorningEnabled()).isTrue();
        assertThat(auditLogRepository.findAll().stream().filter(a -> opted.getId().equals(a.getActorUserId())))
                .extracting(a -> a.getAction()).containsExactly(AuditAction.STAFF_THERAPIST_MORNING_CHANGED);

        clock.set(DAY + "T07:59");
        scanService.runOnce();
        assertThat(notificationsOf(opted, "THERAPIST_MORNING")).isEmpty();

        clock.set(DAY + "T08:00");
        scanService.runOnce();
        scanService.runOnce();
        assertThat(notificationsOf(opted, "THERAPIST_MORNING")).hasSize(1);
        assertThat(notificationsOf(opted, "THERAPIST_MORNING").get(0).getText()).isEqualTo("Good morning! Procedures today: 2, the first at 11:30.");
        assertThat(notificationsOf(notOpted, "THERAPIST_MORNING")).as("never chose, default off").isEmpty();
        assertThat(notificationsOf(idle, "THERAPIST_MORNING")).as("opted in but nothing today").isEmpty();

        // The feature itself can be switched off, and the default can be flipped.
        botSettings(s -> s.setTherapistMorningDefaultOn(true));
        scanService.runOnce();
        assertThat(notificationsOf(notOpted, "THERAPIST_MORNING")).hasSize(1);
        botSettings(s -> s.setTherapistMorningEnabled(false));
        clock.set(TOMORROW + "T09:00");
        scanService.runOnce();
        assertThat(notificationsOf(opted, "THERAPIST_MORNING")).hasSize(1);
    }

    // ---- who is on shift ----------------------------------------------------------------------------------------------

    @Test
    void theBoard_inWords_byDepartment_andAMuteTerminalIsSaidFirstInsteadOfCallingEverybodyLate() {
        UserEntity onShift = user("Olga OnShift", Role.WAITER);
        UserEntity late = user("Lena Late", Role.WAITER);
        onShift.setStaffArea(com.sunsetbeach.model.StaffArea.RESTAURANT);
        late.setStaffArea(com.sunsetbeach.model.StaffArea.RESTAURANT);
        userRepository.saveAndFlush(onShift);
        userRepository.saveAndFlush(late);
        ShiftCode code = shiftCode("09:00", "18:00", manager);
        roster(onShift, code, manager);
        roster(late, code, manager);
        punchIn(onShift, "09:02", manager);
        var terminal = healthyDevice("Front desk terminal");

        say(managerChat, "/onshift");
        String healthy = telegram.lastTo(managerChat).text();
        assertThat(healthy).doesNotContain("terminal").contains("Late: Lena Late").contains("On shift: Olga OnShift");

        // The terminal stops answering: nobody's scans can be seen, so "late" would be an accusation.
        touchDevice(terminal, DAY + "T09:00");
        say(managerChat, "/onshift");
        String mute = telegram.lastTo(managerChat).text();
        assertThat(mute.lines().findFirst().orElseThrow()).startsWith("! The attendance terminal is not responding (Front desk terminal)");
        assertThat(mute).doesNotContain("Late:").doesNotContain("Absent:").contains("On the roster: Lena Late, Olga OnShift");
        // The morning summary says the same in its staff line.
        assertThat(summaryService.render(summaryService.collect(botSettingsRepository.findById(1).orElseThrow()), com.sunsetbeach.model.NotificationLanguage.EN))
                .contains("The attendance terminal is not responding");

        // With no active terminal at all it says that instead.
        deviceRepository.findById(terminal.getId()).ifPresent(d -> {
            d.setActive(false);
            deviceRepository.saveAndFlush(d);
        });
        say(managerChat, "/onshift");
        assertThat(telegram.lastTo(managerChat).text().lines().findFirst().orElseThrow()).startsWith("! No attendance terminal is active");
    }

    // ---- the summary ---------------------------------------------------------------------------------------------------

    @Test
    void theSummary_agreesWithTheAdminEndpointsItReadsFrom_figureByFigure() throws Exception {
        // A day with every kind of figure in it: an arrival still expected, one who arrived, one leaving today, an overdue
        // guest, a blocked room, a dirty room, a late and an absent... and a waiting shift request.
        RoomUnitEntity expected = roomUnit();
        RoomUnitEntity arrived = roomUnit();
        RoomUnitEntity leaving = roomUnit();
        RoomUnitEntity overdue = roomUnit();
        RoomUnitEntity blocked = roomUnit();
        RoomUnitEntity dirty = roomUnit();
        booking(expected, "Alice Arriving", TODAY, TODAY.plusDays(2));
        Booking arrivedBooking = booking(arrived, "Bob Arrived", TODAY, TODAY.plusDays(2));
        Booking leavingBooking = booking(leaving, "Cora Leaving", TODAY.minusDays(1), TODAY);
        Booking overdueBooking = booking(overdue, "Dan Overdue", TODAY.minusDays(3), TODAY.minusDays(1));
        actAs(manager);
        occupancyService.checkIn(arrivedBooking.getId());
        occupancyService.checkIn(leavingBooking.getId());
        occupancyService.checkIn(overdueBooking.getId());
        roomUnitService.createBlock(blocked.getId(), new RoomUnitBlockInput(TODAY.toString(), TODAY.plusDays(1).toString(), "Pipe"), manager.getId());
        SecurityContextHolderClear.clear();
        var dirtyUnit = roomUnitRepository.findById(dirty.getId()).orElseThrow();
        dirtyUnit.setHousekeepingStatus(HousekeepingStatus.DIRTY);
        roomUnitRepository.saveAndFlush(dirtyUnit);
        blockRepository.findByRoomUnitId(blocked.getId()).forEach(b -> blockIds.add(b.getId()));

        UserEntity worker = user("Wendy Worker", Role.WAITER);
        UserEntity tardy = user("Tom Tardy", Role.WAITER);
        UserEntity requester = user("Rita Requester", Role.WAITER);
        ShiftCode code = shiftCode("09:00", "18:00", manager);
        roster(worker, code, manager);
        roster(tardy, code, manager);
        punchIn(worker, "09:01", manager);
        healthyDevice("Terminal");
        actAs(manager);
        rosterService.createEntry(new com.sunsetbeach.model.RosterEntryCreateInput(requester.getId(), TODAY.plusDays(3).toString(), code.getId()), manager.getId());
        SecurityContextHolderClear.clear();
        var entry = rosterEntryRepository.findByEmployeeUserIdAndDate(requester.getId(), TODAY.plusDays(3)).orElseThrow();
        requestService.submit(requester, entry.getId(), ShiftChangeRequestType.DELETE, null, "family", ShiftChangeRequestChannel.TELEGRAM);

        StaffSummaryService.Data data = summaryService.collect(botSettingsRepository.findById(1).orElseThrow());

        JsonNode board = json("/bookings/today");
        JsonNode report = json("/reports/manager?date=" + DAY).get("today");
        JsonNode map = json("/property-map");
        JsonNode attendance = json("/attendance/today");
        JsonNode pending = json("/shift-change-requests?status=PENDING");

        int overdueOnBoard = (int) stream(board.get("departingToday")).filter(e -> e.get("overdueDays").asInt() > 0).count();
        assertThat(data.arrivals().expected()).isEqualTo(board.get("arrivingToday").size()).isEqualTo(1);
        assertThat(data.arrivals().total()).isEqualTo(report.get("accounts").get("arrivals").asInt()).isEqualTo(2);
        assertThat(data.departures().overdue()).isEqualTo(overdueOnBoard).isEqualTo(1);
        assertThat(data.departures().due()).isEqualTo(board.get("departingToday").size() - overdueOnBoard).isEqualTo(1);
        assertThat(data.departures().total()).isEqualTo(report.get("accounts").get("departures").asInt()).isEqualTo(1);
        assertThat(data.inHouse().rooms()).isEqualTo(board.get("inHouse").size()).isEqualTo(3);
        assertThat(data.inHouse().guests()).isEqualTo(report.get("guests").get("guestsInHouse").asInt());
        JsonNode rooms = report.get("rooms");
        assertThat(data.occupancy().percent()).isEqualTo(rooms.get("occupancyPercent").isNull() ? null : rooms.get("occupancyPercent").asText());
        assertThat(data.occupancy().free()).isEqualTo(rooms.get("availableForSale").asInt() - rooms.get("occupied").asInt());
        assertThat(data.rooms().blocked()).isEqualTo(rooms.get("outOfOrder").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(data.rooms().dirty()).isEqualTo((int) stream(map.get("units"))
                .filter(u -> u.get("isActive").asBoolean() && "DIRTY".equals(u.get("housekeepingStatus").asText())).count()).isGreaterThanOrEqualTo(1);
        assertThat(data.staff().terminalOk()).isTrue();
        assertThat(data.staff().rostered()).isEqualTo((int) stream(attendance).filter(s -> !s.path("unscheduled").asBoolean(false)).count());
        assertThat(data.staff().onShift()).isEqualTo((int) stream(attendance).filter(s -> "ON_SHIFT".equals(s.get("state").asText())).count());
        assertThat(data.staff().absent()).isEqualTo((int) stream(attendance).filter(s -> "MISSED".equals(s.get("state").asText())).count());
        assertThat(data.pendingRequests()).isEqualTo(pending.size()).isEqualTo(1);

        // And what a manager reads in the bot is those figures, with no guest's name in it.
        say(managerChat, "/summary");
        String text = telegram.lastTo(managerChat).text();
        assertThat(text).contains("Arrivals: 1 still expected (2 booked for today).")
                .contains("Overdue departures (still in the house past check-out): 1.")
                .contains("Shift requests waiting for a decision: 1.");
        assertThat(text).doesNotContain("Alice").doesNotContain("Bob").doesNotContain("Cora").doesNotContain("Dan");
    }

    @Test
    void theSummaryBlocks_areSwitchedOnePerBlock() {
        botSettings(s -> {
            s.setSummaryArrivals(false);
            s.setSummaryDepartures(false);
            s.setSummaryInHouse(false);
            s.setSummaryOccupancy(false);
            s.setSummaryRooms(false);
            s.setSummaryStaff(false);
        });

        say(managerChat, "/summary");
        String text = telegram.lastTo(managerChat).text();
        assertThat(text).contains("Shift requests waiting for a decision").doesNotContain("Arrivals").doesNotContain("Departures").doesNotContain("In the house")
                .doesNotContain("Occupancy").doesNotContain("Rooms blocked").doesNotContain("Staff:");

        botSettings(s -> s.setSummaryRequests(false));
        say(managerChat, "/summary");
        assertThat(telegram.lastTo(managerChat).text()).contains("No blocks are switched on for the summary.");
    }

    @Test
    void theMorningSummary_goesToTheDirectorsOncePerDay_inTheirLanguage_afterTheTime() {
        UserEntity director = user("Dina Director", Role.MANAGER);
        UserEntity russian = user("Dima Director", Role.MANAGER);
        russian.setNotificationLanguage("RU");
        userRepository.saveAndFlush(russian);
        UserEntity bystander = user("Bea Bystander", Role.MANAGER);
        settings(s -> {
            s.setDirectorUserIds(new String[] {director.getId(), russian.getId()});
            s.setQuietHoursEnabled(false);
        });
        botSettings(s -> s.setMorningSummaryEnabled(true));

        clock.set(DAY + "T07:59");
        scanService.runOnce();
        assertThat(notificationsOf(director, "MORNING_SUMMARY")).as("not before the time").isEmpty();

        clock.set(DAY + "T08:00");
        scanService.runOnce();
        scanService.runOnce();
        clock.set(DAY + "T17:30");
        scanService.runOnce();
        assertThat(notificationsOf(director, "MORNING_SUMMARY")).hasSize(1);
        assertThat(notificationsOf(russian, "MORNING_SUMMARY")).hasSize(1);
        assertThat(notificationsOf(bystander, "MORNING_SUMMARY")).isEmpty();
        assertThat(notificationsOf(director, "MORNING_SUMMARY").get(0).getText()).startsWith("Summary for ").contains("Arrivals:");
        assertThat(notificationsOf(russian, "MORNING_SUMMARY").get(0).getText()).startsWith("Сводка на ").contains("Заезды:");

        // The next hotel day it goes again - once.
        clock.set(TOMORROW + "T09:00");
        scanService.runOnce();
        scanService.runOnce();
        assertThat(notificationsOf(director, "MORNING_SUMMARY")).hasSize(2);

        // Switched off: nothing more.
        botSettings(s -> s.setMorningSummaryEnabled(false));
        clock.set(TODAY.plusDays(2) + "T09:00");
        scanService.runOnce();
        assertThat(notificationsOf(director, "MORNING_SUMMARY")).hasSize(2);
    }

    // ---- the HR director's own request ----------------------------------------------------------------------------------

    @Test
    void theHrDirector_cannotApproveTheirOwnRequest_neitherByButtonNorByApi_anAdministratorDoes() throws Exception {
        UserEntity hr = user("Hana HR", Role.WAITER, "HR");
        String hrChat = link(hr);
        UserEntity admin = user("Alex Admin", Role.ADMIN);
        String adminChat = link(admin);
        ShiftCode code = shiftCode("09:00", "18:00", manager);
        LocalDate shiftDay = TODAY.plusDays(3);
        actAs(manager);
        rosterService.createEntry(new com.sunsetbeach.model.RosterEntryCreateInput(hr.getId(), shiftDay.toString(), code.getId()), manager.getId());
        SecurityContextHolderClear.clear();
        var entry = rosterEntryRepository.findByEmployeeUserIdAndDate(hr.getId(), shiftDay).orElseThrow();

        ShiftChangeRequestEntity request = requestService.submit(hr, entry.getId(), ShiftChangeRequestType.DELETE, null, "family", ShiftChangeRequestChannel.TELEGRAM);

        // Announced to the administrators with buttons; the HR director gets neither the message nor a button.
        assertThat(notificationsOf(admin, "SHIFT_REQUEST_NEW")).hasSize(1);
        assertThat(notificationsOf(hr, "SHIFT_REQUEST_NEW")).isEmpty();
        assertThat(staffActionRepository.findAll().stream().filter(a -> hr.getId().equals(a.getUserId()) && request.getId().equals(a.getSubjectId()))).isEmpty();
        assertThat(staffActionRepository.findAll().stream().filter(a -> admin.getId().equals(a.getUserId()) && request.getId().equals(a.getSubjectId()))).hasSize(2);

        // The service refuses the HR director's decision, whichever way it comes.
        assertThatThrownBy(() -> requestService.decide(request.getId(), hr, true, null, ShiftChangeRequestChannel.TELEGRAM)).isInstanceOf(ForbiddenException.class);
        // A button that somehow exists for them (forged, or from before the request was theirs) does nothing.
        press(hrChat, button(hr, com.sunsetbeach.notification.NotificationAction.Kinds.SHIFT_REQUEST_APPROVE, request.getId()));
        assertThat(telegram.answers).last().isEqualTo("You can't do that.");
        say(hrChat, "/pending");
        assertThat(telegram.lastTo(hrChat).text()).isEqualTo("No requests are waiting.");
        // Over HTTP: the HR director is a decider for others' requests, not for this one.
        mockMvc.perform(as(post("/shift-change-requests/" + request.getId() + "/decision").contentType(MediaType.APPLICATION_JSON)
                .content("{\"decision\":\"APPROVE\"}"), hr)).andExpect(status().isForbidden());
        mockMvc.perform(as(get("/shift-change-requests?employeeUserId=" + hr.getId()), hr))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].callerMayDecide").value(false))
                .andExpect(jsonPath("$[0].requiresAdministrator").value(true));
        mockMvc.perform(as(get("/shift-change-requests?employeeUserId=" + hr.getId()), admin))
                .andExpect(jsonPath("$[0].callerMayDecide").value(true));
        assertThat(shiftRequestRepository.findById(request.getId()).orElseThrow().getStatus()).isEqualTo("PENDING");
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(hr.getId(), shiftDay)).isPresent();

        // The administrator's own list shows it, and their approval is what applies it.
        say(adminChat, "/pending");
        assertThat(telegram.lastTo(adminChat).text()).contains("Hana HR");
        assertThat(requestService.decide(request.getId(), admin, true, null, ShiftChangeRequestChannel.TELEGRAM).changedNow()).isTrue();
        assertThat(rosterEntryRepository.findByEmployeeUserIdAndDate(hr.getId(), shiftDay)).isEmpty();
    }

    @Test
    void theHrDirectorsOwnRequest_goesToTheAdministrators_evenWhenTheFallbackIsOff() {
        UserEntity hr = user("Hana HR", Role.WAITER, "HR");
        UserEntity admin = linkedAdmin("Alex Admin");
        UserEntity employee = user("Eve Employee", Role.WAITER);
        ShiftCode code = shiftCode("09:00", "18:00", manager);
        botSettings(s -> s.setFallbackToAdmin(false));
        actAs(manager);
        rosterService.createEntry(new com.sunsetbeach.model.RosterEntryCreateInput(hr.getId(), TODAY.plusDays(4).toString(), code.getId()), manager.getId());
        rosterService.createEntry(new com.sunsetbeach.model.RosterEntryCreateInput(employee.getId(), TODAY.plusDays(4).toString(), code.getId()), manager.getId());
        SecurityContextHolderClear.clear();

        var own = requestService.submit(hr, rosterEntryRepository.findByEmployeeUserIdAndDate(hr.getId(), TODAY.plusDays(4)).orElseThrow().getId(),
                ShiftChangeRequestType.DELETE, null, "x", ShiftChangeRequestChannel.TELEGRAM);
        var others = requestService.submit(employee, rosterEntryRepository.findByEmployeeUserIdAndDate(employee.getId(), TODAY.plusDays(4)).orElseThrow().getId(),
                ShiftChangeRequestType.DELETE, null, "y", ShiftChangeRequestChannel.TELEGRAM);

        assertThat(deciderService.mayDecide(admin, hr.getId(), TODAY)).isTrue();
        assertThat(deciderService.mayDecide(hr, hr.getId(), TODAY)).isFalse();
        assertThat(notificationsOf(admin, "SHIFT_REQUEST_NEW")).hasSize(1); // only the HR director's own
        assertThat(notificationsOf(hr, "SHIFT_REQUEST_NEW")).hasSize(1); // only the employee's
        assertThat(deciderService.mayDecide(admin, employee.getId(), TODAY)).as("fallback off: administrators do not decide others' requests").isFalse();
        assertThat(requestService.decide(own.getId(), admin, false, null, ShiftChangeRequestChannel.ADMIN_UI).request().getStatus()).isEqualTo("REJECTED");
        assertThat(others.getStatus()).isEqualTo("PENDING");
    }

    // ---- small helpers ---------------------------------------------------------------------------------------------------

    private JsonNode json(String url) throws Exception {
        String body = mockMvc.perform(as(get(url), manager)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body);
    }

    private static java.util.stream.Stream<JsonNode> stream(JsonNode array) {
        return java.util.stream.StreamSupport.stream(array.spliterator(), false);
    }

    /** The tests set a SecurityContext only to let the services' own audit calls write; clear it again. */
    private static final class SecurityContextHolderClear {
        static void clear() {
            org.springframework.security.core.context.SecurityContextHolder.clearContext();
        }
    }
}
