package com.sunsetbeach.service;

import static com.sunsetbeach.service.GymHallSettingsService.HHMM;
import static com.sunsetbeach.service.GymHallSettingsService.MAX_DAILY_MINUTES;
import static com.sunsetbeach.service.GymHallSettingsService.MAX_DURATION_MINUTES;
import static com.sunsetbeach.service.GymHallSettingsService.MIN_DURATION_MINUTES;
import static com.sunsetbeach.service.GymHallSettingsService.STEP_MINUTES;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.entity.GymHallSettingsEntity;
import com.sunsetbeach.entity.GymReservationActionEntity;
import com.sunsetbeach.entity.GymReservationEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.SqlStates;
import com.sunsetbeach.error.UnauthorizedException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.GymGuestInfo;
import com.sunsetbeach.model.GymGuestReservation;
import com.sunsetbeach.model.GymGuestReservationInput;
import com.sunsetbeach.model.GymGuestStay;
import com.sunsetbeach.model.GymReservation;
import com.sunsetbeach.model.GymReservationCreateInput;
import com.sunsetbeach.model.GymReservationScheduleInput;
import com.sunsetbeach.model.GymReservationStatus;
import com.sunsetbeach.model.GymSchedule;
import com.sunsetbeach.model.GymSlot;
import com.sunsetbeach.model.GymSlotDay;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.repository.GymReservationActionRepository;
import com.sunsetbeach.repository.GymReservationRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reserving the gym for a hotel booking - by the guest from their own account, or by staff for a
 * guest. The gym is exclusive: the database (V142's exclusion constraint) decides who got a slot,
 * there is no check-then-write. What the code checks itself is everything the database cannot:
 * the grid, the opening hours, the stay, who may book, and the 120 minutes a day per booking
 * (serialised by locking the booking row, so two simultaneous bookings of one booking cannot both
 * pass). The equipment that follows a reservation is {@link GymAutomationService}'s business.
 *
 * <p>A reservation never changes the money side: the gym is free, nothing is billed.
 */
@Service
public class GymReservationService {

    private static final List<GymReservationStatus> HOLDING = List.of(GymReservationStatus.BOOKED, GymReservationStatus.COMPLETED);
    private static final int MAX_SCHEDULE_DAYS = 14;

    private final GymReservationRepository reservations;
    private final GymReservationActionRepository actions;
    private final GymHallSettingsService hallSettings;
    private final BookingRepository bookings;
    private final GuestAccountRepository guestAccounts;
    private final RoomUnitRepository roomUnits;
    private final UserRepository users;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public GymReservationService(
            GymReservationRepository reservations,
            GymReservationActionRepository actions,
            GymHallSettingsService hallSettings,
            BookingRepository bookings,
            GuestAccountRepository guestAccounts,
            RoomUnitRepository roomUnits,
            UserRepository users,
            AuditLogService auditLogService,
            Clock clock) {
        this.reservations = reservations;
        this.actions = actions;
        this.hallSettings = hallSettings;
        this.bookings = bookings;
        this.guestAccounts = guestAccounts;
        this.roomUnits = roomUnits;
        this.users = users;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    /** Who is acting: a staff member (CASHIER+), a guest through their account, or the system (cascades). */
    private record Actor(String label, String userId, String guestAccountId, boolean staff) {
        static Actor staff(String userId, String name) {
            return new Actor(name, userId, null, true);
        }

        static Actor guest(String accountId, String email) {
            return new Actor("guest " + email, null, accountId, false);
        }

        static Actor system() {
            return new Actor("system", null, null, false);
        }
    }

    // ---- staff ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public GymSchedule schedule(String fromText, String toText, boolean includeCancelled) {
        LocalDate from = parseDate(fromText, "from");
        LocalDate to = parseDate(toText, "to");
        if (to.isBefore(from)) {
            throw new BadRequestException("'to' must not be before 'from'");
        }
        if (ChronoUnit.DAYS.between(from, to) >= MAX_SCHEDULE_DAYS) {
            throw new BadRequestException("The schedule can show at most " + MAX_SCHEDULE_DAYS + " days at a time");
        }
        List<GymReservationEntity> found = includeCancelled
                ? reservations.findByDateBetweenOrderByDateAscStartTimeAsc(from, to)
                : reservations.findByDateBetweenAndStatusInOrderByDateAscStartTimeAsc(from, to, HOLDING);
        GymHallSettingsEntity hours = hallSettings.entity();
        return new GymSchedule(
                from.toString(), to.toString(), hours.getOpenTime().format(HHMM), hours.getCloseTime().format(HHMM),
                STEP_MINUTES, MIN_DURATION_MINUTES, MAX_DURATION_MINUTES, MAX_DAILY_MINUTES, toStaffDtos(found));
    }

    @Transactional
    public GymReservation createForStaff(GymReservationCreateInput input, String userId) {
        Actor actor = Actor.staff(userId, staffName(userId));
        GymReservationEntity saved = insert(input.getBookingId(), parseDate(input.getDate(), "date"), parseTime(input.getStartTime()),
                input.getDurationMinutes(), actor);
        return toStaffDtos(List.of(saved)).get(0);
    }

    @Transactional
    public GymReservation move(String id, GymReservationScheduleInput input, String userId) {
        Actor actor = Actor.staff(userId, staffName(userId));
        GymReservationEntity reservation = lockReservation(id);
        if (reservation.getStatus() != GymReservationStatus.BOOKED) {
            throw new ConflictException("Only a booked reservation can be moved");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        if (!reservation.end().isAfter(now)) {
            throw new ConflictException("This reservation is already over");
        }
        LocalDate date = parseDate(input.getDate(), "date");
        LocalTime start = parseTime(input.getStartTime());
        boolean automationStarted = actions.existsByReservationId(id);
        boolean sameStart = date.equals(reservation.getDate()) && start.equals(reservation.getStartTime());
        if (automationStarted && !sameStart) {
            throw new ConflictException("The equipment has already been switched on for this reservation — cancel it and book again. "
                    + "Only the length of a session in progress can be changed.");
        }
        BookingEntity booking = bookings.findById(reservation.getBookingId()).orElseThrow(() -> new NotFoundException("Booking not found"));
        validateSlot(date, start, input.getDurationMinutes(), true, automationStarted);
        if (automationStarted && !date.atTime(start).plusMinutes(input.getDurationMinutes()).isAfter(now)) {
            throw new BadRequestException("The new length would end the session in the past — cancel the reservation instead");
        }
        requireServable(booking, date, true);
        requireDailyAllowance(booking.getId(), date, input.getDurationMinutes(), id);

        String before = describe(reservation);
        reservation.setDate(date);
        reservation.setStartTime(start);
        reservation.setDurationMinutes(input.getDurationMinutes());
        GymReservationEntity saved = flushOrConflict(reservation);
        audit(actor, AuditAction.GYM_RESERVATION_UPDATED, saved.getId(),
                "Gym reservation of " + booking.getGuestName() + " moved from " + before + " to " + describe(saved));
        return toStaffDtos(List.of(saved)).get(0);
    }

    @Transactional
    public GymReservation cancelByStaff(String id, String reason, String userId) {
        Actor actor = Actor.staff(userId, staffName(userId));
        GymReservationEntity reservation = lockReservation(id);
        requireCancellable(reservation);
        cancel(reservation, actor, reason == null || reason.isBlank() ? null : reason.trim());
        return toStaffDtos(List.of(reservation)).get(0);
    }

    // ---- the hotel booking ends --------------------------------------------------------------

    /**
     * A cancelled booking, or a guest who checked out, takes every reservation of the booking that
     * has not ended with it - the future ones and one in progress (the guest is gone). Runs inside
     * the booking operation's own transaction, like the spa's cascade; a session that was already
     * switched on is switched off by the automation's next run.
     */
    @Transactional
    public int onBookingEnded(BookingEntity booking, String reason) {
        LocalDateTime now = LocalDateTime.now(clock);
        Actor actor = isStaffRequest() ? Actor.staff(staffId(), staffName(staffId())) : Actor.system();
        int cancelled = 0;
        for (GymReservationEntity reservation : reservations.findByBookingId(booking.getId())) {
            if (reservation.getStatus() == GymReservationStatus.BOOKED && reservation.end().isAfter(now)) {
                cancel(reservation, actor, reason);
                cancelled++;
            }
        }
        return cancelled;
    }

    // ---- guest ------------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public GymGuestInfo guestInfo(String accountId) {
        List<BookingEntity> all = accountBookings(accountId);
        List<BookingEntity> eligible = all.stream().filter(GymReservationService::isCheckedIn).toList();
        GymHallSettingsEntity hours = hallSettings.entity();
        GymGuestInfo info = new GymGuestInfo(
                !eligible.isEmpty(), stays(eligible), hours.getOpenTime().format(HHMM), hours.getCloseTime().format(HHMM),
                STEP_MINUTES, MIN_DURATION_MINUTES, MAX_DURATION_MINUTES, MAX_DAILY_MINUTES);
        if (eligible.isEmpty()) {
            info.message(notCheckedInMessage(all));
        }
        return info;
    }

    @Transactional(readOnly = true)
    public GymSlotDay guestSlots(String accountId, String dateText, String bookingId) {
        LocalDate date = parseDate(dateText, "date");
        BookingEntity booking = resolveGuestBooking(accountId, bookingId);
        requireWithinStay(booking, date);
        GymHallSettingsEntity hours = hallSettings.entity();
        LocalDateTime now = LocalDateTime.now(clock);

        List<GymReservationEntity> dayReservations = reservations.findByDateBetweenAndStatusInOrderByDateAscStartTimeAsc(date, date, HOLDING);
        int used = dayReservations.stream().filter(r -> r.getBookingId().equals(booking.getId())).mapToInt(GymReservationEntity::getDurationMinutes).sum();
        int remaining = Math.max(0, MAX_DAILY_MINUTES - used);

        int openMin = minutesOfDay(hours.getOpenTime());
        int closeMin = minutesOfDay(hours.getCloseTime());
        List<GymSlot> slots = new ArrayList<>();
        for (int m = openMin; m + STEP_MINUTES <= closeMin; m += STEP_MINUTES) {
            LocalTime start = LocalTime.of(m / 60, m % 60);
            LocalDateTime startAt = date.atTime(start);
            GymReservationEntity covering = dayReservations.stream()
                    .filter(r -> r.start().isBefore(startAt.plusMinutes(STEP_MINUTES)) && r.end().isAfter(startAt))
                    .findFirst().orElse(null);
            GymSlot.StatusEnum status;
            int maxDuration = 0;
            if (startAt.isBefore(now)) {
                status = GymSlot.StatusEnum.PAST;
            } else if (covering != null) {
                status = GymSlot.StatusEnum.TAKEN;
            } else if (remaining < STEP_MINUTES) {
                status = GymSlot.StatusEnum.DAILY_LIMIT;
            } else {
                status = GymSlot.StatusEnum.FREE;
                int untilClose = closeMin - m;
                int untilNext = dayReservations.stream()
                        .filter(r -> r.start().isAfter(startAt))
                        .mapToInt(r -> (int) ChronoUnit.MINUTES.between(startAt, r.start()))
                        .min().orElse(Integer.MAX_VALUE);
                int longest = Math.min(Math.min(MAX_DURATION_MINUTES, remaining), Math.min(untilClose, untilNext));
                maxDuration = longest / STEP_MINUTES * STEP_MINUTES;
            }
            slots.add(new GymSlot(start.format(HHMM), status, maxDuration));
        }
        return new GymSlotDay(date.toString(), booking.getId(), hours.getOpenTime().format(HHMM), hours.getCloseTime().format(HHMM),
                STEP_MINUTES, remaining, slots);
    }

    @Transactional
    public GymGuestReservation createForGuest(String accountId, GymGuestReservationInput input) {
        BookingEntity booking = resolveGuestBooking(accountId, input.getBookingId());
        Actor actor = Actor.guest(accountId, guestEmail(accountId));
        GymReservationEntity saved = insert(booking.getId(), parseDate(input.getDate(), "date"), parseTime(input.getStartTime()),
                input.getDurationMinutes(), actor);
        return toGuestDto(saved, LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public List<GymGuestReservation> listForGuest(String accountId) {
        Set<String> bookingIds = accountBookings(accountId).stream().map(BookingEntity::getId).collect(Collectors.toSet());
        if (bookingIds.isEmpty()) {
            return List.of();
        }
        LocalDateTime now = LocalDateTime.now(clock);
        return reservations.findByBookingIdIn(bookingIds).stream()
                .sorted(Comparator.comparing(GymReservationEntity::start).reversed())
                .map(r -> toGuestDto(r, now))
                .toList();
    }

    @Transactional
    public GymGuestReservation cancelForGuest(String accountId, String id) {
        GymReservationEntity reservation = lockReservation(id);
        Set<String> own = accountBookings(accountId).stream().map(BookingEntity::getId).collect(Collectors.toSet());
        if (!own.contains(reservation.getBookingId())) {
            // Someone else's reservation looks exactly like a missing one.
            throw new NotFoundException("Reservation not found");
        }
        LocalDateTime now = LocalDateTime.now(clock);
        requireCancellable(reservation);
        if (!reservation.start().isAfter(now)) {
            throw new ConflictException("This session has already started — ask the front desk if you need to change it");
        }
        cancel(reservation, Actor.guest(accountId, guestEmail(accountId)), null);
        return toGuestDto(reservation, now);
    }

    // ---- creation core ----------------------------------------------------------------------

    private GymReservationEntity insert(String bookingId, LocalDate date, LocalTime start, int duration, Actor actor) {
        validateSlot(date, start, duration, actor.staff(), false);
        // The row lock is what makes the daily allowance race-free: every create/move of one
        // booking queues here, so the sum below is read after the previous one committed.
        BookingEntity booking = bookings.findByIdForUpdate(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        requireServable(booking, date, actor.staff());
        requireDailyAllowance(booking.getId(), date, duration, null);

        GymReservationEntity entity = new GymReservationEntity();
        entity.setBookingId(booking.getId());
        entity.setDate(date);
        entity.setStartTime(start);
        entity.setDurationMinutes(duration);
        entity.setCreatedByUserId(actor.userId());
        entity.setCreatedByGuestAccountId(actor.guestAccountId());
        GymReservationEntity saved = flushOrConflict(entity);
        audit(actor, AuditAction.GYM_RESERVATION_CREATED, saved.getId(),
                "Gym reserved for " + booking.getGuestName() + ": " + describe(saved));
        return saved;
    }

    /** The grid, duration, opening hours and "not in the past" - everything checkable from the request alone. */
    private void validateSlot(LocalDate date, LocalTime start, int duration, boolean staff, boolean alreadyRunning) {
        if (duration < MIN_DURATION_MINUTES || duration > MAX_DURATION_MINUTES || duration % STEP_MINUTES != 0) {
            throw new BadRequestException("A session lasts 30, 60, 90 or 120 minutes");
        }
        if (start.getMinute() % STEP_MINUTES != 0 || start.getSecond() != 0 || start.getNano() != 0) {
            throw new BadRequestException("A session must start on the half hour (:00 or :30)");
        }
        GymHallSettingsEntity hours = hallSettings.entity();
        int startMin = minutesOfDay(start);
        if (startMin < minutesOfDay(hours.getOpenTime())) {
            throw new BadRequestException("The gym opens at " + hours.getOpenTime().format(HHMM));
        }
        if (startMin + duration > minutesOfDay(hours.getCloseTime())) {
            throw new BadRequestException("The session would run past closing time (" + hours.getCloseTime().format(HHMM) + ")");
        }
        if (alreadyRunning) {
            return;
        }
        LocalDateTime startAt = date.atTime(start);
        LocalDateTime now = LocalDateTime.now(clock);
        // A guest books a start that is still ahead; the front desk may also take the half hour that is running now.
        boolean past = staff ? !startAt.plusMinutes(STEP_MINUTES).isAfter(now) : startAt.isBefore(now);
        if (past) {
            throw new BadRequestException("That time has already passed");
        }
    }

    /** Who the gym can be booked for, on which date. {@code staff} may also book a guest who has not arrived yet. */
    private static void requireServable(BookingEntity booking, LocalDate date, boolean staff) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new ConflictException(booking.getGuestName() + "'s booking is cancelled, so the gym cannot be booked for it");
        }
        switch (booking.getOccupancyStatus()) {
            case CHECKED_OUT -> throw new ConflictException(booking.getGuestName() + " has already checked out, so the gym cannot be booked for this booking");
            case NO_SHOW -> throw new ConflictException(booking.getGuestName() + " was marked as a no-show, so the gym cannot be booked for this booking");
            case EXPECTED -> {
                if (!staff) {
                    throw new ForbiddenException(CHECK_IN_FIRST);
                }
            }
            case CHECKED_IN -> {
            }
        }
        requireWithinStay(booking, date);
    }

    private static void requireWithinStay(BookingEntity booking, LocalDate date) {
        if (date.isBefore(booking.getCheckIn()) || date.isAfter(booking.getCheckOut())) {
            throw new BadRequestException("The stay is " + booking.getCheckIn() + " to " + booking.getCheckOut()
                    + ", which does not include " + date + " — pick a date within the stay");
        }
    }

    private void requireDailyAllowance(String bookingId, LocalDate date, int duration, String exceptReservationId) {
        int used = reservations.findByBookingIdAndDateAndStatusIn(bookingId, date, HOLDING).stream()
                .filter(r -> !r.getId().equals(exceptReservationId))
                .mapToInt(GymReservationEntity::getDurationMinutes)
                .sum();
        if (used + duration > MAX_DAILY_MINUTES) {
            int left = Math.max(0, MAX_DAILY_MINUTES - used);
            throw new ConflictException(left == 0
                    ? "The " + MAX_DAILY_MINUTES + " minutes of gym time for " + date + " are already booked for this stay"
                    : "Only " + left + " minutes of gym time are left for " + date + " on this stay");
        }
    }

    // ---- shared helpers ---------------------------------------------------------------------

    private GymReservationEntity lockReservation(String id) {
        String bookingId = reservations.findBookingIdById(id).orElseThrow(() -> new NotFoundException("Reservation not found"));
        // Booking first, reservation second: the same order a create takes, so they cannot deadlock.
        bookings.findByIdForUpdate(bookingId);
        return reservations.findById(id).orElseThrow(() -> new NotFoundException("Reservation not found"));
    }

    private void requireCancellable(GymReservationEntity reservation) {
        if (reservation.getStatus() != GymReservationStatus.BOOKED) {
            throw new ConflictException(reservation.getStatus() == GymReservationStatus.CANCELLED
                    ? "This reservation is already cancelled"
                    : "This reservation is already over");
        }
        if (!reservation.end().isAfter(LocalDateTime.now(clock))) {
            throw new ConflictException("This reservation is already over");
        }
    }

    /** Marks a reservation cancelled; with nothing sent to the equipment there is nothing left for the automation. */
    private void cancel(GymReservationEntity reservation, Actor actor, String reason) {
        reservation.setStatus(GymReservationStatus.CANCELLED);
        reservation.setCancelledAt(TimestampFormat.nowUtc(clock));
        reservation.setCancelledBy(actor.label());
        reservation.setCancelReason(reason);
        if (!actions.existsByReservationId(reservation.getId())) {
            reservation.setAutomationDone(true);
        }
        GymReservationEntity saved = reservations.saveAndFlush(reservation);
        String guest = bookings.findById(saved.getBookingId()).map(BookingEntity::getGuestName).orElse("a guest");
        audit(actor, AuditAction.GYM_RESERVATION_CANCELLED, saved.getId(),
                "Gym reservation of " + guest + " (" + describe(saved) + ") cancelled" + (reason != null ? " — " + reason : ""));
    }

    private GymReservationEntity flushOrConflict(GymReservationEntity entity) {
        try {
            return reservations.saveAndFlush(entity);
        } catch (DataAccessException e) {
            throw translateOverlap(e);
        }
    }

    /** By SQLSTATE, never by message text - see {@link SqlStates} and CLAUDE.md's Concurrency section. */
    static ConflictException translateOverlap(DataAccessException e) {
        if (SqlStates.is(e, "40P01") || SqlStates.is(e, "40001")) {
            return new ConflictException("Someone else was changing the gym schedule at the same time — please try again.");
        }
        if (SqlStates.is(e, "23P01")) {
            return new ConflictException("The gym is already booked for part of that time.");
        }
        throw e;
    }

    private void audit(Actor actor, AuditAction action, String entityId, String summary) {
        if (actor.staff() && isStaffRequest()) {
            auditLogService.record(action, AuditEntityType.GYM_RESERVATION, entityId, summary);
        } else {
            auditLogService.recordSystemAction(action, AuditEntityType.GYM_RESERVATION, entityId,
                    actor.staff() || "system".equals(actor.label()) ? summary : summary + " (by " + actor.label() + ")");
        }
    }

    private static boolean isStaffRequest() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof StaffPrincipal;
    }

    private static String staffId() {
        return ((StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).id();
    }

    private String staffName(String userId) {
        return users.findById(userId)
                .map(u -> u.getFullName() != null && !u.getFullName().isBlank() ? u.getFullName().trim() : u.getName())
                .orElse("staff");
    }

    private static String describe(GymReservationEntity r) {
        return r.getDate() + " " + r.getStartTime().format(HHMM) + "–" + r.end().toLocalTime().format(HHMM);
    }

    private static int minutesOfDay(LocalTime t) {
        return t.getHour() * 60 + t.getMinute();
    }

    private static LocalDate parseDate(String text, String field) {
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BadRequestException(field + " must be a date like 2026-10-14");
        }
    }

    private static LocalTime parseTime(String text) {
        try {
            return LocalTime.parse(text, HHMM);
        } catch (DateTimeParseException | NullPointerException e) {
            throw new BadRequestException("startTime must be a time like 18:30");
        }
    }

    // ---- guest's bookings -------------------------------------------------------------------

    static final String CHECK_IN_FIRST = "The gym can be booked once you have checked in at the front desk.";

    /** The same set {@code GuestAccountService#listBookings} returns: the account's own card's bookings, else by email. */
    private List<BookingEntity> accountBookings(String accountId) {
        GuestAccountEntity account = guestAccounts.findById(accountId).orElseThrow(() -> new UnauthorizedException("Account no longer exists"));
        return account.getGuestId() != null
                ? bookings.findByGuestIdOrderByCreatedAtDesc(account.getGuestId())
                : bookings.findByGuestEmailIgnoreCaseOrderByCreatedAtDesc(account.getEmail());
    }

    private static boolean isCheckedIn(BookingEntity b) {
        return b.getStatus() != BookingStatus.CANCELLED && b.getOccupancyStatus() == OccupancyStatus.CHECKED_IN;
    }

    private BookingEntity resolveGuestBooking(String accountId, String bookingId) {
        List<BookingEntity> all = accountBookings(accountId);
        List<BookingEntity> eligible = all.stream().filter(GymReservationService::isCheckedIn).toList();
        if (bookingId != null && all.stream().noneMatch(b -> b.getId().equals(bookingId))) {
            throw new NotFoundException("Booking not found");
        }
        if (eligible.isEmpty()) {
            throw new ForbiddenException(notCheckedInMessage(all));
        }
        if (bookingId != null) {
            return eligible.stream().filter(b -> b.getId().equals(bookingId)).findFirst()
                    .orElseThrow(() -> new ForbiddenException(CHECK_IN_FIRST));
        }
        if (eligible.size() > 1) {
            throw new BadRequestException("You have more than one booking in the house — choose which one the session is for");
        }
        return eligible.get(0);
    }

    private static String notCheckedInMessage(List<BookingEntity> all) {
        if (all.isEmpty()) {
            return "You have no booking yet. " + CHECK_IN_FIRST;
        }
        return CHECK_IN_FIRST;
    }

    private List<GymGuestStay> stays(List<BookingEntity> eligible) {
        Map<String, String> labels = roomLabels(eligible);
        return eligible.stream()
                .map(b -> new GymGuestStay(b.getId(), labels.get(b.getId()), b.getCheckIn().toString(), b.getCheckOut().toString()))
                .toList();
    }

    // ---- DTOs -------------------------------------------------------------------------------

    private GymGuestReservation toGuestDto(GymReservationEntity r, LocalDateTime now) {
        return new GymGuestReservation(
                r.getId(), r.getBookingId(), r.getDate().toString(), r.getStartTime().format(HHMM), r.end().toLocalTime().format(HHMM),
                r.getDurationMinutes(), r.getStatus(), r.getStatus() == GymReservationStatus.BOOKED && r.start().isAfter(now));
    }

    private List<GymReservation> toStaffDtos(List<GymReservationEntity> list) {
        if (list.isEmpty()) {
            return List.of();
        }
        Map<String, BookingEntity> bookingsById = bookings.findAllById(list.stream().map(GymReservationEntity::getBookingId).distinct().toList())
                .stream().collect(Collectors.toMap(BookingEntity::getId, b -> b));
        Map<String, String> labels = roomLabels(bookingsById.values());
        Set<String> staffIds = list.stream().map(GymReservationEntity::getCreatedByUserId).filter(java.util.Objects::nonNull).collect(Collectors.toSet());
        Map<String, UserEntity> staff = users.findAllById(staffIds).stream().collect(Collectors.toMap(UserEntity::getId, u -> u));
        Map<String, List<GymReservationActionEntity>> actionRows = actions.findByReservationIdIn(list.stream().map(GymReservationEntity::getId).toList())
                .stream().collect(Collectors.groupingBy(GymReservationActionEntity::getReservationId));

        List<GymReservation> result = new ArrayList<>();
        for (GymReservationEntity r : list) {
            BookingEntity booking = bookingsById.get(r.getBookingId());
            boolean byGuest = r.getCreatedByGuestAccountId() != null;
            UserEntity creator = byGuest ? null : staff.get(r.getCreatedByUserId());
            String creatorName = byGuest ? "Guest" : creator == null ? "Staff" : creator.getFullName() != null && !creator.getFullName().isBlank()
                    ? creator.getFullName().trim() : creator.getName();
            GymReservation dto = new GymReservation(
                    r.getId(), r.getBookingId(), booking == null ? "" : booking.getGuestName(), r.getDate().toString(),
                    r.getStartTime().format(HHMM), r.end().toLocalTime().format(HHMM), r.getDurationMinutes(), r.getStatus(),
                    byGuest ? GymReservation.CreatedByTypeEnum.GUEST : GymReservation.CreatedByTypeEnum.STAFF, creatorName,
                    GymAutomationView.of(actionRows.getOrDefault(r.getId(), List.of())), TimestampFormat.toUtc(r.getCreatedAt()));
            String label = labels.get(r.getBookingId());
            if (label != null) {
                dto.roomLabel(label);
            }
            if (r.getCancelledBy() != null) {
                dto.cancelledBy(r.getCancelledBy());
            }
            if (r.getCancelReason() != null) {
                dto.cancelReason(r.getCancelReason());
            }
            result.add(dto);
        }
        return result;
    }

    /** Booking id to the label of its assigned room (null entries are simply absent). */
    private Map<String, String> roomLabels(Collection<BookingEntity> list) {
        Map<String, String> unitIdByBooking = new HashMap<>();
        for (BookingEntity b : list) {
            if (b.getRoomUnitId() != null) {
                unitIdByBooking.put(b.getId(), b.getRoomUnitId());
            }
        }
        Map<String, String> labelByUnit = roomUnits.findAllById(unitIdByBooking.values()).stream()
                .collect(Collectors.toMap(RoomUnitEntity::getId, RoomUnitEntity::getLabel));
        Map<String, String> result = new HashMap<>();
        unitIdByBooking.forEach((bookingId, unitId) -> {
            String label = labelByUnit.get(unitId);
            if (label != null) {
                result.put(bookingId, label);
            }
        });
        return result;
    }

    private String guestEmail(String accountId) {
        return guestAccounts.findById(accountId).map(GuestAccountEntity::getEmail).orElse("guest");
    }
}
