package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.GymGuestInfo;
import com.sunsetbeach.model.GymGuestReservation;
import com.sunsetbeach.model.GymGuestReservationInput;
import com.sunsetbeach.model.GymReservation;
import com.sunsetbeach.model.GymReservationCreateInput;
import com.sunsetbeach.model.GymReservationStatus;
import com.sunsetbeach.model.GymSlot;
import com.sunsetbeach.model.GymSlotDay;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.repository.GymReservationRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.support.MutableClock;
import com.sunsetbeach.tuya.TuyaClient;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Who may reserve the gym and under which rules, against a real database and a clock the test
 * holds still at 2031-06-15 10:00 (hotel time): a guest needs a {@code CHECKED_IN} booking of their
 * own account, the slot must be inside the stay, on the grid, inside the opening hours and not in
 * the past, a booking gets 120 minutes a day, the gym holds one session at a time (decided by the
 * exclusion constraint, so a real race has one winner), and someone else's reservation cannot be
 * cancelled. Not {@code @Transactional}: the races need real commits; everything is removed in
 * {@code @AfterEach}.
 */
@SpringBootTest(properties = {"app.gym.automation.interval-ms=3600000"})
class GymReservationServiceTests extends AbstractIntegrationTest {

    private static final String TODAY = "2031-06-15";
    private static final String TOMORROW = "2031-06-16";

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(TODAY + "T10:00");
        }
    }

    @Autowired private MutableClock clock;
    @Autowired private GymReservationService service;
    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private GymReservationRepository reservations;
    @Autowired private BookingRepository bookings;
    @Autowired private RoomRepository rooms;
    @Autowired private GuestAccountRepository accounts;
    @Autowired private UserRepository users;
    @Autowired private AuditLogRepository auditLogRepository;
    @MockitoBean private TuyaClient tuya;

    private RoomEntity room;
    private UserEntity staff;
    private GuestAccountEntity guestA;
    private GuestAccountEntity guestB;
    private GuestAccountEntity guestNotArrived;
    private BookingEntity bookingA;
    private BookingEntity bookingB;
    private BookingEntity bookingNotArrived;

    @BeforeEach
    void setUp() {
        clock.set(TODAY + "T10:00");
        reservations.deleteAll();
        RoomEntity r = new RoomEntity();
        r.setName("Gym Reservation Room " + UUID.randomUUID());
        r.setDescription("Used only by GymReservationServiceTests");
        r.setCapacity(2);
        r.setBasePrice(new BigDecimal("1000.00"));
        room = rooms.saveAndFlush(r);
        UserEntity u = new UserEntity();
        u.setEmail("gym-res-" + UUID.randomUUID() + "@test.local");
        u.setName(u.getEmail());
        u.setFullName("Desk Clerk");
        u.setPasswordHash("unused");
        u.setRole(Role.CASHIER);
        u.setActive(true);
        staff = users.saveAndFlush(u);
        guestA = account("alice");
        guestB = account("bob");
        guestNotArrived = account("carol");
        bookingA = booking(guestA, OccupancyStatus.CHECKED_IN, "2031-06-14", "2031-06-18");
        bookingB = booking(guestB, OccupancyStatus.CHECKED_IN, "2031-06-14", "2031-06-18");
        bookingNotArrived = booking(guestNotArrived, OccupancyStatus.EXPECTED, "2031-06-15", "2031-06-18");
    }

    @AfterEach
    void cleanUp() {
        reservations.deleteAll();
        bookings.deleteAll(bookings.findAll().stream().filter(b -> room != null && room.getId().equals(b.getRoomId())).toList());
        if (room != null) {
            rooms.deleteById(room.getId());
        }
        accounts.deleteAll(accounts.findAll().stream().filter(a -> a.getEmail().endsWith("@gym-test.local")).toList());
        if (staff != null) {
            users.deleteById(staff.getId());
        }
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                .filter(a -> a.getEntityType() == AuditEntityType.GYM_RESERVATION || a.getEntityType() == AuditEntityType.BOOKING)
                .filter(a -> a.getSummary() != null && (a.getSummary().contains("Gym") || a.getSummary().contains("gym")))
                .toList());
    }

    // ---- who may book ------------------------------------------------------------------------

    @Test
    void aGuestWithoutACheckedInBooking_isRefused_everywhere() {
        GymGuestInfo info = service.guestInfo(guestNotArrived.getId());
        assertThat(info.getEligible()).isFalse();
        assertThat(info.getMessage().get()).contains("checked in");

        assertThatThrownBy(() -> service.createForGuest(guestNotArrived.getId(), guestInput(null, TOMORROW, "12:00", 60)))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("checked in");
        assertThatThrownBy(() -> service.guestSlots(guestNotArrived.getId(), TOMORROW, null)).isInstanceOf(ForbiddenException.class);
        assertThat(reservations.findAll()).isEmpty();
    }

    @Test
    void aGuestCannotBookWithSomeoneElsesBookingId() {
        assertThatThrownBy(() -> service.createForGuest(guestA.getId(), guestInput(bookingB.getId(), TOMORROW, "12:00", 60)))
                .isInstanceOf(NotFoundException.class);
        assertThat(reservations.findAll()).isEmpty();
    }

    @Test
    void aCheckedInGuest_books_andSeesItInTheirList() {
        GymGuestReservation made = service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "12:00", 90));

        assertThat(made.getStartTime()).isEqualTo("12:00");
        assertThat(made.getEndTime()).isEqualTo("13:30");
        assertThat(made.getStatus()).isEqualTo(GymReservationStatus.BOOKED);
        assertThat(made.getCanCancel()).isTrue();
        assertThat(service.listForGuest(guestA.getId())).extracting(GymGuestReservation::getId).containsExactly(made.getId());
        assertThat(service.listForGuest(guestB.getId())).isEmpty();
    }

    @Test
    void staffCanBookAGuestWhoHasNotArrived_aGuestCannotBookForThemselves() {
        GymReservation made = service.createForStaff(
                new GymReservationCreateInput(bookingNotArrived.getId(), TOMORROW, "09:00", 30), staff.getId());

        assertThat(made.getGuestName()).isEqualTo("carol");
        assertThat(made.getCreatedByType()).isEqualTo(GymReservation.CreatedByTypeEnum.STAFF);
        assertThat(made.getCreatedByName()).isEqualTo("Desk Clerk");
    }

    @Test
    void aCancelledOrCheckedOutBooking_cannotBeBooked_evenByStaff() {
        BookingEntity cancelled = booking(guestA, OccupancyStatus.CHECKED_IN, "2031-06-14", "2031-06-18");
        cancelled.setStatus(BookingStatus.CANCELLED);
        bookings.saveAndFlush(cancelled);
        BookingEntity gone = booking(guestA, OccupancyStatus.CHECKED_OUT, "2031-06-10", "2031-06-15");

        assertThatThrownBy(() -> service.createForStaff(new GymReservationCreateInput(cancelled.getId(), TOMORROW, "09:00", 30), staff.getId()))
                .isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.createForStaff(new GymReservationCreateInput(gone.getId(), TODAY, "15:00", 30), staff.getId()))
                .isInstanceOf(ConflictException.class);
    }

    // ---- slot rules --------------------------------------------------------------------------

    @Test
    void theSlotMustBeInsideTheStay() {
        assertThatThrownBy(() -> service.createForGuest(guestA.getId(), guestInput(null, "2031-06-19", "12:00", 60)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("stay");
        // The departure day itself is still part of the stay.
        assertThat(service.createForGuest(guestA.getId(), guestInput(null, "2031-06-18", "12:00", 60)).getDate()).isEqualTo("2031-06-18");
    }

    @Test
    void aGuestCannotBookAStartThatHasPassed_staffMayTakeTheHalfHourThatIsRunning() {
        clock.set(TODAY + "T10:10");

        assertThatThrownBy(() -> service.createForGuest(guestA.getId(), guestInput(null, TODAY, "10:00", 30)))
                .isInstanceOf(BadRequestException.class);
        assertThat(service.createForStaff(new GymReservationCreateInput(bookingA.getId(), TODAY, "10:00", 30), staff.getId()).getStartTime())
                .isEqualTo("10:00");
        assertThatThrownBy(() -> service.createForStaff(new GymReservationCreateInput(bookingB.getId(), TODAY, "09:00", 30), staff.getId()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void gridDurationAndOpeningHours_areEnforced() {
        for (Object[] c : new Object[][] {
            {"12:15", 60},   // off the grid
            {"12:00", 45},   // not a multiple of 30
            {"12:00", 0},
            {"12:00", 150},  // above 120
            {"05:30", 60},   // before the 06:00 opening
            {"21:30", 60},   // would run past the 22:00 closing
        }) {
            assertThatThrownBy(() -> service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, (String) c[0], (Integer) c[1])))
                    .as("%s for %s minutes", c[0], c[1])
                    .isInstanceOf(BadRequestException.class);
        }
        // The boundaries themselves are fine.
        assertThat(service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "06:00", 30)).getStartTime()).isEqualTo("06:00");
        assertThat(service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "21:00", 60)).getEndTime()).isEqualTo("22:00");
    }

    // ---- exclusive gym -----------------------------------------------------------------------

    @Test
    void anOverlap_isRefusedByTheDatabase_adjacentSessionsAreFine() {
        service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "12:00", 60));

        assertThatThrownBy(() -> service.createForGuest(guestB.getId(), guestInput(null, TOMORROW, "12:30", 60)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already booked");
        assertThatThrownBy(() -> service.createForStaff(new GymReservationCreateInput(bookingB.getId(), TOMORROW, "11:30", 60), staff.getId()))
                .isInstanceOf(ConflictException.class);
        assertThat(service.createForGuest(guestB.getId(), guestInput(null, TOMORROW, "11:00", 60)).getEndTime()).isEqualTo("12:00");
        assertThat(service.createForGuest(guestB.getId(), guestInput(null, TOMORROW, "13:00", 30)).getStartTime()).isEqualTo("13:00");
    }

    @Test
    void aCancelledReservation_freesItsSlot() {
        GymGuestReservation first = service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "12:00", 60));
        service.cancelForGuest(guestA.getId(), first.getId());

        assertThat(service.createForGuest(guestB.getId(), guestInput(null, TOMORROW, "12:00", 60)).getStartTime()).isEqualTo("12:00");
    }

    @Test
    void simultaneousBookingsOfTheSameSlot_haveExactlyOneWinner() throws Exception {
        List<BookingEntity> contenders = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            contenders.add(booking(account("racer" + i), OccupancyStatus.CHECKED_IN, "2031-06-14", "2031-06-18"));
        }
        List<Callable<Object>> tasks = contenders.stream()
                .<Callable<Object>>map(b -> () -> service.createForStaff(new GymReservationCreateInput(b.getId(), TOMORROW, "15:00", 60), staff.getId()))
                .toList();

        List<Object> outcomes = race(tasks);

        assertThat(outcomes.stream().filter(o -> o instanceof GymReservation)).hasSize(1);
        assertThat(outcomes.stream().filter(o -> o instanceof ConflictException)).hasSize(5);
        assertThat(reservations.findAll()).hasSize(1);
    }

    // ---- 120 minutes a day -------------------------------------------------------------------

    @Test
    void aBookingGetsAtMost120MinutesADay_perHotelBooking() {
        service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "12:00", 60));
        service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "14:00", 60));

        assertThatThrownBy(() -> service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "16:00", 30)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("120");
        // Another day has its own allowance, and so does another booking.
        assertThat(service.createForGuest(guestA.getId(), guestInput(null, "2031-06-17", "16:00", 120)).getDurationMinutes()).isEqualTo(120);
        assertThat(service.createForGuest(guestB.getId(), guestInput(null, TOMORROW, "16:00", 120)).getDurationMinutes()).isEqualTo(120);
    }

    @Test
    void theAllowanceCountsAcrossStaffAndGuestBookings_andACancelledOneGivesItBack() {
        GymReservation byStaff = service.createForStaff(new GymReservationCreateInput(bookingA.getId(), TOMORROW, "12:00", 90), staff.getId());

        assertThatThrownBy(() -> service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "15:00", 60)))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("30 minutes");
        assertThat(service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "15:00", 30)).getStartTime()).isEqualTo("15:00");

        service.cancelByStaff(byStaff.getId(), "changed plans", staff.getId());
        assertThat(service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "17:00", 90)).getDurationMinutes()).isEqualTo(90);
    }

    @Test
    void simultaneousBookingsOfOneHotelBooking_cannotExceedTheDailyAllowance() throws Exception {
        List<Callable<Object>> tasks = new ArrayList<>();
        for (String start : List.of("08:00", "10:00", "12:00", "14:00")) {
            tasks.add(() -> service.createForStaff(new GymReservationCreateInput(bookingA.getId(), TOMORROW, start, 60), staff.getId()));
        }

        List<Object> outcomes = race(tasks);

        assertThat(outcomes.stream().filter(o -> o instanceof GymReservation)).hasSize(2);
        assertThat(reservations.findAll().stream().mapToInt(r -> r.getDurationMinutes()).sum()).isEqualTo(120);
    }

    // ---- cancelling --------------------------------------------------------------------------

    @Test
    void aGuestCannotCancelSomeoneElsesReservation() {
        GymGuestReservation mine = service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "12:00", 60));

        assertThatThrownBy(() -> service.cancelForGuest(guestB.getId(), mine.getId())).isInstanceOf(NotFoundException.class);
        assertThat(reservations.findById(mine.getId()).orElseThrow().getStatus()).isEqualTo(GymReservationStatus.BOOKED);
    }

    @Test
    void aGuestCancelsTheirOwnBeforeItStarts_notAfter_andNotTwice() {
        GymGuestReservation soon = service.createForGuest(guestA.getId(), guestInput(null, TODAY, "11:00", 60));
        GymGuestReservation cancelled = service.cancelForGuest(guestA.getId(), soon.getId());
        assertThat(cancelled.getStatus()).isEqualTo(GymReservationStatus.CANCELLED);
        assertThat(cancelled.getCanCancel()).isFalse();
        assertThatThrownBy(() -> service.cancelForGuest(guestA.getId(), soon.getId())).isInstanceOf(ConflictException.class);

        GymGuestReservation later = service.createForGuest(guestA.getId(), guestInput(null, TODAY, "13:00", 60));
        clock.set(TODAY + "T13:10");
        assertThatThrownBy(() -> service.cancelForGuest(guestA.getId(), later.getId()))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("already started");
    }

    @Test
    void staffCanCancelASessionInProgress() {
        GymReservation r = service.createForStaff(new GymReservationCreateInput(bookingA.getId(), TODAY, "10:00", 60), staff.getId());

        GymReservation cancelled = service.cancelByStaff(r.getId(), "equipment broken", staff.getId());

        assertThat(cancelled.getStatus()).isEqualTo(GymReservationStatus.CANCELLED);
        assertThat(cancelled.getCancelReason().get()).isEqualTo("equipment broken");
        assertThat(cancelled.getCancelledBy().get()).isEqualTo("Desk Clerk");
    }

    // ---- the hotel booking ends --------------------------------------------------------------

    @Test
    void cancellingTheHotelBooking_cancelsItsFutureGymReservations_andOnlyThose() {
        // A guest who has not arrived is the one whose booking can still be cancelled (a checked-in stay is checked out instead).
        GymReservation first = service.createForStaff(new GymReservationCreateInput(bookingNotArrived.getId(), TOMORROW, "09:00", 30), staff.getId());
        GymReservation second = service.createForStaff(new GymReservationCreateInput(bookingNotArrived.getId(), "2031-06-17", "15:00", 60), staff.getId());
        GymGuestReservation theirs = service.createForGuest(guestB.getId(), guestInput(null, TOMORROW, "14:00", 60));

        bookingService.updateStatus(bookingNotArrived.getId(), new BookingStatusInput(BookingStatus.CANCELLED));

        for (String id : List.of(first.getId(), second.getId())) {
            assertThat(reservations.findById(id).orElseThrow().getStatus()).isEqualTo(GymReservationStatus.CANCELLED);
            assertThat(reservations.findById(id).orElseThrow().getCancelledBy()).isEqualTo("system");
        }
        assertThat(reservations.findById(theirs.getId()).orElseThrow().getStatus()).isEqualTo(GymReservationStatus.BOOKED);
        // The slot is free again.
        assertThat(service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "09:00", 30)).getStartTime()).isEqualTo("09:00");
    }

    @Test
    void aRealCheckOut_takesTheGuestsFutureGymSessionsWithIt() {
        clock.set(TODAY + "T11:00");
        GymReservation tomorrow = service.createForStaff(new GymReservationCreateInput(bookingA.getId(), TOMORROW, "09:00", 30), staff.getId());
        GymReservation other = service.createForStaff(new GymReservationCreateInput(bookingB.getId(), TOMORROW, "10:00", 30), staff.getId());

        occupancyService.checkOut(bookingA.getId(), null);

        assertThat(reservations.findById(tomorrow.getId()).orElseThrow().getStatus()).isEqualTo(GymReservationStatus.CANCELLED);
        assertThat(reservations.findById(other.getId()).orElseThrow().getStatus()).isEqualTo(GymReservationStatus.BOOKED);
    }

    @Test
    void whenTheGuestChecksOut_futureAndRunningReservationsGo_pastOnesStay() {
        clock.set(TODAY + "T11:00");
        GymReservation running = service.createForStaff(new GymReservationCreateInput(bookingA.getId(), TODAY, "11:00", 60), staff.getId());
        GymReservation later = service.createForStaff(new GymReservationCreateInput(bookingA.getId(), TODAY, "15:00", 30), staff.getId());
        GymReservation tomorrow = service.createForStaff(new GymReservationCreateInput(bookingA.getId(), TOMORROW, "09:00", 30), staff.getId());
        clock.set(TODAY + "T12:30"); // the first one is over by now

        int cancelled = service.onBookingEnded(bookings.findById(bookingA.getId()).orElseThrow(), "Guest checked out");

        assertThat(cancelled).isEqualTo(2);
        assertThat(reservationOf(later.getId()).getStatus()).isEqualTo(GymReservationStatus.CANCELLED);
        assertThat(reservationOf(tomorrow.getId()).getStatus()).isEqualTo(GymReservationStatus.CANCELLED);
        assertThat(reservationOf(running.getId()).getStatus()).isEqualTo(GymReservationStatus.BOOKED); // already over: the automation completes it, not the cascade
    }

    // ---- slots -------------------------------------------------------------------------------

    @Test
    void theSlotListShowsWhatIsTaken_whatHasPassed_andHowLongEachFreeSlotCanRun() {
        service.createForGuest(guestB.getId(), guestInput(null, TODAY, "12:00", 60));
        service.createForGuest(guestA.getId(), guestInput(null, TODAY, "15:00", 30));

        GymSlotDay day = service.guestSlots(guestA.getId(), TODAY, bookingA.getId());

        assertThat(day.getRemainingMinutes()).isEqualTo(90);
        assertThat(slot(day, "09:30").getStatus()).isEqualTo(GymSlot.StatusEnum.PAST);
        assertThat(slot(day, "10:00").getStatus()).isEqualTo(GymSlot.StatusEnum.FREE);
        assertThat(slot(day, "12:00").getStatus()).isEqualTo(GymSlot.StatusEnum.TAKEN);
        assertThat(slot(day, "12:30").getStatus()).isEqualTo(GymSlot.StatusEnum.TAKEN);
        // 10:00 could run up to 90 more minutes (what is left of the day) but the 12:00 session stops it at 120 -> capped by allowance.
        assertThat(slot(day, "10:00").getMaxDurationMinutes()).isEqualTo(90);
        // 11:00 is cut short by the session at 12:00.
        assertThat(slot(day, "11:00").getMaxDurationMinutes()).isEqualTo(60);
        assertThat(slot(day, "11:30").getMaxDurationMinutes()).isEqualTo(30);
        assertThat(slot(day, "21:30").getMaxDurationMinutes()).isEqualTo(30);
    }

    @Test
    void aSpentAllowance_showsDailyLimitOnEveryFreeSlot() {
        service.createForGuest(guestA.getId(), guestInput(null, TOMORROW, "12:00", 120));

        GymSlotDay day = service.guestSlots(guestA.getId(), TOMORROW, bookingA.getId());

        assertThat(day.getRemainingMinutes()).isZero();
        assertThat(slot(day, "08:00").getStatus()).isEqualTo(GymSlot.StatusEnum.DAILY_LIMIT);
        assertThat(slot(day, "12:00").getStatus()).isEqualTo(GymSlot.StatusEnum.TAKEN);
    }

    // ---- helpers -----------------------------------------------------------------------------

    private GymReservation reservationOf(String id) {
        return service.schedule(TODAY, TOMORROW, true).getReservations().stream().filter(r -> r.getId().equals(id)).findFirst().orElseThrow();
    }

    private static GymSlot slot(GymSlotDay day, String start) {
        return day.getSlots().stream().filter(s -> s.getStartTime().equals(start)).findFirst().orElseThrow();
    }

    private static GymGuestReservationInput guestInput(String bookingId, String date, String start, int minutes) {
        GymGuestReservationInput in = new GymGuestReservationInput(date, start, minutes);
        if (bookingId != null) {
            in.bookingId(bookingId);
        }
        return in;
    }

    private List<Object> race(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CyclicBarrier barrier = new CyclicBarrier(tasks.size());
        try {
            List<Future<Object>> futures = new ArrayList<>();
            for (Callable<Object> task : tasks) {
                futures.add(pool.submit(() -> {
                    barrier.await();
                    try {
                        return task.call();
                    } catch (Exception e) {
                        return e;
                    }
                }));
            }
            List<Object> outcomes = new ArrayList<>();
            for (Future<Object> f : futures) {
                outcomes.add(f.get());
            }
            return outcomes;
        } finally {
            pool.shutdownNow();
        }
    }

    private GuestAccountEntity account(String name) {
        GuestAccountEntity a = new GuestAccountEntity();
        a.setEmail(name + "-" + UUID.randomUUID() + "@gym-test.local");
        a.setPasswordHash("unused");
        a.setName(name);
        a.setEmailVerifiedAt(java.time.LocalDateTime.of(2031, 1, 1, 0, 0));
        return accounts.saveAndFlush(a);
    }

    /** A booking linked to the account the way the guest history falls back to: by email. */
    private BookingEntity booking(GuestAccountEntity account, OccupancyStatus occupancy, String checkIn, String checkOut) {
        BookingEntity b = new BookingEntity();
        b.setChannel(BookingChannel.WALK_IN);
        b.setRoomId(room.getId());
        b.setGuestName(account.getName());
        b.setGuestEmail(account.getEmail());
        b.setGuestPhone("+66800000000");
        b.setCheckIn(LocalDate.parse(checkIn));
        b.setCheckOut(LocalDate.parse(checkOut));
        b.setTotalPrice(new BigDecimal("1000.00"));
        b.setStatus(BookingStatus.CONFIRMED);
        b.setOccupancyStatus(occupancy);
        return bookings.saveAndFlush(b);
    }
}
