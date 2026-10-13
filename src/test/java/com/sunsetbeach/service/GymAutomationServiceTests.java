package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.GymDeviceEntity;
import com.sunsetbeach.entity.GymReservationEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.GymAutomationPhase;
import com.sunsetbeach.model.GymReservation;
import com.sunsetbeach.model.GymReservationScheduleInput;
import com.sunsetbeach.model.GymReservationStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.GymDeviceRepository;
import com.sunsetbeach.repository.GymReservationActionRepository;
import com.sunsetbeach.repository.GymReservationRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.support.MutableClock;
import com.sunsetbeach.tuya.TuyaClient;
import com.sunsetbeach.tuya.TuyaException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;
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
 * The equipment automation around gym reservations, driven by a clock the test moves and Tuya
 * mocked at {@link TuyaClient}: switch-on lead minutes ahead, switch-off at the end, two
 * back-to-back sessions without a switch-off between, a backend restart in the middle of a
 * session, a session cancelled after the switch-on, and a device that keeps failing. Every
 * assertion on "no command" is also an assertion that nothing polls (state is never read).
 *
 * <p>Not {@code @Transactional}: the automation reads and writes in its own transactions, so
 * everything is committed and removed again in {@code @AfterEach}. Reservations are in 2031, far
 * from any real date another test context's own sweep could look at.
 */
@SpringBootTest(properties = {"app.gym.automation.interval-ms=3600000", "app.gym.automation.retry-pause-seconds=0"})
class GymAutomationServiceTests extends AbstractIntegrationTest {

    private static final String DAY = "2031-06-15";

    @TestConfiguration
    static class ClockConfig {
        @Bean
        @Primary
        MutableClock mutableClock() {
            return new MutableClock(DAY + "T08:00");
        }
    }

    @Autowired private MutableClock clock;
    @Autowired private GymAutomationService automation;
    @Autowired private GymReservationService reservationService;
    @Autowired private GymHallSettingsService hallSettings;
    @Autowired private GymReservationRepository reservations;
    @Autowired private GymReservationActionRepository actions;
    @Autowired private GymDeviceRepository devices;
    @Autowired private BookingRepository bookings;
    @Autowired private RoomRepository rooms;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private GymDeviceService gymDeviceService;
    @MockitoBean private TuyaClient tuya;

    private RoomEntity room;
    private BookingEntity booking;
    private BookingEntity otherBooking;
    private GymDeviceEntity fan1;
    private GymDeviceEntity fan2;
    private GymDeviceEntity ac;

    @BeforeEach
    void setUp() {
        reset(tuya);
        clock.set(DAY + "T08:00");
        wipe();
        RoomEntity r = new RoomEntity();
        r.setName("Gym Automation Room " + UUID.randomUUID());
        r.setDescription("Used only by GymAutomationServiceTests");
        r.setCapacity(2);
        r.setBasePrice(new BigDecimal("1000.00"));
        room = rooms.saveAndFlush(r);
        booking = booking("Auto Guest A");
        otherBooking = booking("Auto Guest B");
        fan1 = relay("Fan 1", "tuya-fan-1", 0);
        fan2 = relay("Fan 2", "tuya-fan-2", 1);
        ac = airConditioner("Air 1", "tuya-ac-1", 2);
    }

    @AfterEach
    void cleanUp() {
        wipe();
        if (booking != null) {
            bookings.deleteById(booking.getId());
            bookings.deleteById(otherBooking.getId());
            rooms.deleteById(room.getId());
        }
        auditLogRepository.deleteAll(auditLogRepository.findAll().stream()
                .filter(a -> a.getEntityType() == AuditEntityType.GYM_RESERVATION || a.getEntityType() == AuditEntityType.GYM_DEVICE)
                .toList());
    }

    @Autowired private com.sunsetbeach.repository.GymHallSettingsRepository hallSettingsRepository;

    private void resetSettings() {
        com.sunsetbeach.entity.GymHallSettingsEntity e = hallSettingsRepository.findById(1).orElseThrow();
        e.setOpenTime(LocalTime.of(6, 0));
        e.setCloseTime(LocalTime.of(22, 0));
        e.setLeadMinutes(30);
        e.setAcMode(0);
        e.setAcTemp(16);
        e.setAcWind(3);
        hallSettingsRepository.saveAndFlush(e);
    }

    private void wipe() {
        resetSettings();
        reservations.deleteAll();
        devices.deleteAll();
        gymDeviceService.clearStateCache();
    }

    // ---- the plain cycle ---------------------------------------------------------------------

    @Test
    void switchesOn_leadMinutesBeforeStart_andNotBefore() {
        GymReservationEntity r = reserve(booking, "10:00", 60);

        clock.set(DAY + "T09:29");
        automation.runOnce();
        verify(tuya, never()).sendCommand(anyString(), anyString(), any());
        verify(tuya, never()).sendAirConditionerCommand(anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyInt());

        clock.set(DAY + "T09:30");
        automation.runOnce();
        verify(tuya).sendCommand("tuya-fan-1", "switch_1", true);
        verify(tuya).sendCommand("tuya-fan-2", "switch_1", true);
        // The owner's defaults: power on, mode 0 (cool), 16 degrees, wind 3 (strongest), in ONE scene command.
        verify(tuya).sendAirConditionerCommand("hub-1", "tuya-ac-1", 1, 0, 16, 3);
        assertThat(onPhase(r).getState()).isEqualTo(GymAutomationPhase.StateEnum.OK);
        verifyNothingPolled();
    }

    @Test
    void rerunning_doesNotSendTwice() {
        reserve(booking, "10:00", 60);
        clock.set(DAY + "T09:30");
        automation.runOnce();
        clock.set(DAY + "T09:31");
        automation.runOnce();
        clock.set(DAY + "T10:30");
        automation.runOnce();

        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", true);
        verify(tuya, times(1)).sendCommand("tuya-fan-2", "switch_1", true);
        verify(tuya, times(1)).sendAirConditionerCommand("hub-1", "tuya-ac-1", 1, 0, 16, 3);
        verify(tuya, never()).sendCommand(anyString(), anyString(), eq(false));
    }

    @Test
    void switchesOff_atTheEnd_andMarksTheReservationCompleted() {
        GymReservationEntity r = reserve(booking, "10:00", 60);
        clock.set(DAY + "T09:30");
        automation.runOnce();

        clock.set(DAY + "T10:59");
        automation.runOnce();
        verify(tuya, never()).sendCommand(anyString(), anyString(), eq(false));

        clock.set(DAY + "T11:00");
        automation.runOnce();
        verify(tuya).sendCommand("tuya-fan-1", "switch_1", false);
        verify(tuya).sendCommand("tuya-fan-2", "switch_1", false);
        verify(tuya).sendAirConditionerCommand("hub-1", "tuya-ac-1", 0, 0, 16, 3);
        GymReservationEntity after = reservations.findById(r.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(GymReservationStatus.COMPLETED);
        assertThat(after.isAutomationDone()).isTrue();

        // Nothing more, however often it runs.
        clock.set(DAY + "T11:05");
        automation.runOnce();
        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", false);
        verifyNothingPolled();
    }

    @Test
    void theAcScene_followsTheSettings() {
        com.sunsetbeach.model.GymHallSettings s = hallSettings.get();
        s.setLeadMinutes(45);
        s.setAcMode(2);
        s.setAcTemp(22);
        s.setAcWind(1);
        hallSettings.update(s, 0);
        reserve(booking, "10:00", 60);

        clock.set(DAY + "T09:14");
        automation.runOnce();
        verify(tuya, never()).sendCommand(anyString(), anyString(), any());

        clock.set(DAY + "T09:15");
        automation.runOnce();
        verify(tuya).sendAirConditionerCommand("hub-1", "tuya-ac-1", 1, 2, 22, 1);
    }

    @Test
    void aDeviceNotTakingPartInBookings_isLeftAlone() {
        fan2.setInBookings(false);
        devices.saveAndFlush(fan2);
        reserve(booking, "10:00", 30);

        clock.set(DAY + "T09:30");
        automation.runOnce();

        verify(tuya).sendCommand("tuya-fan-1", "switch_1", true);
        verify(tuya, never()).sendCommand(eq("tuya-fan-2"), anyString(), any());
    }

    // ---- two sessions in a row ---------------------------------------------------------------

    @Test
    void backToBackSessions_noSwitchOffBetween_andOneSwitchOnOnly() {
        GymReservationEntity first = reserve(booking, "10:00", 60);
        GymReservationEntity second = reserve(otherBooking, "11:00", 60);

        clock.set(DAY + "T09:30");
        automation.runOnce();
        clock.set(DAY + "T10:30"); // the second session's own switch-on moment
        automation.runOnce();
        clock.set(DAY + "T11:00"); // the first ends exactly as the second starts
        automation.runOnce();

        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", true);
        verify(tuya, never()).sendCommand(anyString(), anyString(), eq(false));
        GymAutomationPhase firstOff = offPhase(first);
        assertThat(firstOff.getState()).isEqualTo(GymAutomationPhase.StateEnum.SKIPPED);
        assertThat(firstOff.getResults().get(0).getSkipReason().get().getValue()).isEqualTo("NEXT_BOOKING_SOON");
        assertThat(onPhase(second).getResults()).allSatisfy(res -> assertThat(res.getSkipReason().get().getValue()).isEqualTo("COVERED_BY_PREVIOUS"));

        clock.set(DAY + "T12:00");
        automation.runOnce();
        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", false);
        verify(tuya, times(1)).sendCommand("tuya-fan-2", "switch_1", false);
        verify(tuya, times(1)).sendAirConditionerCommand("hub-1", "tuya-ac-1", 0, 0, 16, 3);
        assertThat(offPhase(second).getState()).isEqualTo(GymAutomationPhase.StateEnum.OK);
    }

    @Test
    void aGapWithinTheLeadTime_keepsTheEquipmentOn() {
        GymReservationEntity first = reserve(booking, "10:00", 60);
        reserve(otherBooking, "11:30", 30); // exactly 30 minutes after the first ends: in reach

        clock.set(DAY + "T09:30");
        automation.runOnce();
        clock.set(DAY + "T11:00");
        automation.runOnce();

        verify(tuya, never()).sendCommand(anyString(), anyString(), eq(false));
        assertThat(offPhase(first).getState()).isEqualTo(GymAutomationPhase.StateEnum.SKIPPED);
    }

    @Test
    void aLongerGap_switchesOffBetween_andOnAgainForTheSecondSession() {
        GymReservationEntity first = reserve(booking, "10:00", 30);
        GymReservationEntity second = reserve(otherBooking, "11:30", 30); // 60 minutes after the first ends

        clock.set(DAY + "T09:30");
        automation.runOnce();
        clock.set(DAY + "T10:30");
        automation.runOnce();
        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", false);
        assertThat(offPhase(first).getState()).isEqualTo(GymAutomationPhase.StateEnum.OK);

        clock.set(DAY + "T11:00"); // the second session's switch-on moment: the equipment is off, so it sends its own
        automation.runOnce();
        verify(tuya, times(2)).sendCommand("tuya-fan-1", "switch_1", true);
        assertThat(onPhase(second).getState()).isEqualTo(GymAutomationPhase.StateEnum.OK);

        clock.set(DAY + "T12:00");
        automation.runOnce();
        verify(tuya, times(2)).sendCommand("tuya-fan-1", "switch_1", false);
    }

    // ---- restart and catching up -------------------------------------------------------------

    @Test
    void restartInTheMiddleOfASession_repeatsNothing_andStillSwitchesOffAtTheEnd() {
        GymReservationEntity r = reserve(booking, "10:00", 60);
        clock.set(DAY + "T09:30");
        automation.runOnce();

        // "Restart": a brand-new service instance, no memory except the database.
        GymAutomationService restarted = newAutomationService();
        clock.set(DAY + "T10:20");
        restarted.runOnce();
        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", true);
        verify(tuya, never()).sendCommand(anyString(), anyString(), eq(false));

        clock.set(DAY + "T11:00");
        restarted.runOnce();
        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", false);
        assertThat(reservations.findById(r.getId()).orElseThrow().isAutomationDone()).isTrue();
    }

    @Test
    void aMissedSwitchOn_isCaughtUpWhileTheSessionIsStillRunning() {
        reserve(booking, "10:00", 60);
        // The backend was down from 09:00 and comes back mid-session.
        GymAutomationService restarted = newAutomationService();
        clock.set(DAY + "T10:20");
        restarted.runOnce();

        verify(tuya).sendCommand("tuya-fan-1", "switch_1", true);
        verify(tuya).sendAirConditionerCommand("hub-1", "tuya-ac-1", 1, 0, 16, 3);
    }

    @Test
    void aMissedSwitchOn_isNotSentOnceTheSessionIsOver() {
        GymReservationEntity r = reserve(booking, "10:00", 60);
        GymAutomationService restarted = newAutomationService();
        clock.set(DAY + "T11:30");
        restarted.runOnce();

        verify(tuya, never()).sendCommand(anyString(), anyString(), any());
        verify(tuya, never()).sendAirConditionerCommand(anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyInt());
        GymAutomationPhase on = onPhase(r);
        assertThat(on.getState()).isEqualTo(GymAutomationPhase.StateEnum.SKIPPED);
        assertThat(on.getResults().get(0).getSkipReason().get().getValue()).isEqualTo("MISSED");
        GymReservationEntity after = reservations.findById(r.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(GymReservationStatus.COMPLETED);
        assertThat(after.isAutomationDone()).isTrue();
    }

    // ---- cancellation ------------------------------------------------------------------------

    @Test
    void cancelledAfterSwitchOn_isSwitchedOffRightAway() {
        GymReservationEntity r = reserve(booking, "10:00", 60);
        clock.set(DAY + "T09:30");
        automation.runOnce();

        clock.set(DAY + "T09:45");
        reservationService.cancelByStaff(r.getId(), "guest changed their mind", userId);
        automation.runOnce();

        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", false);
        verify(tuya, times(1)).sendCommand("tuya-fan-2", "switch_1", false);
        verify(tuya, times(1)).sendAirConditionerCommand("hub-1", "tuya-ac-1", 0, 0, 16, 3);
        GymReservationEntity after = reservations.findById(r.getId()).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(GymReservationStatus.CANCELLED);
        assertThat(after.isAutomationDone()).isTrue();
    }

    @Test
    void cancelledBeforeSwitchOn_neverTouchesTheEquipment() {
        GymReservationEntity r = reserve(booking, "10:00", 60);
        clock.set(DAY + "T09:00");
        reservationService.cancelByStaff(r.getId(), null, userId);

        clock.set(DAY + "T09:30");
        automation.runOnce();
        clock.set(DAY + "T11:00");
        automation.runOnce();

        verify(tuya, never()).sendCommand(anyString(), anyString(), any());
        verify(tuya, never()).sendAirConditionerCommand(anyString(), anyString(), anyInt(), anyInt(), anyInt(), anyInt());
    }

    @Test
    void cancelledAfterSwitchOn_butAnotherSessionIsAboutToStart_keepsTheEquipmentOn() {
        GymReservationEntity first = reserve(booking, "10:00", 60);
        reserve(otherBooking, "11:00", 60);
        clock.set(DAY + "T09:30");
        automation.runOnce();

        clock.set(DAY + "T10:40");
        reservationService.cancelByStaff(first.getId(), null, userId);
        automation.runOnce();

        verify(tuya, never()).sendCommand(anyString(), anyString(), eq(false));
        assertThat(offPhase(first).getState()).isEqualTo(GymAutomationPhase.StateEnum.SKIPPED);
    }

    @Test
    void aSessionInProgressCanBeExtended_butNotMovedOnceSwitchedOn() {
        GymReservationEntity r = reserve(booking, "10:00", 60);
        clock.set(DAY + "T09:30");
        automation.runOnce();

        clock.set(DAY + "T10:30");
        GymReservation extended = reservationService.move(r.getId(),
                new GymReservationScheduleInput(DAY, "10:00", 90), userId);
        assertThat(extended.getEndTime()).isEqualTo("11:30");

        org.junit.jupiter.api.Assertions.assertThrows(com.sunsetbeach.error.ConflictException.class, () ->
                reservationService.move(r.getId(), new GymReservationScheduleInput(DAY, "12:00", 60), userId));

        clock.set(DAY + "T11:00");
        automation.runOnce();
        verify(tuya, never()).sendCommand(anyString(), anyString(), eq(false));
        clock.set(DAY + "T11:30");
        automation.runOnce();
        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", false);
    }

    @Test
    void aStayShortenedPastARunningSession_cancelsIt_andTheEquipmentGoesOff() {
        GymReservationEntity r = reserve(booking, "10:00", 60);
        clock.set(DAY + "T09:30");
        automation.runOnce();
        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", true);

        // The stay is cut so that today is no longer part of it (the dates change through the booking, as updateSchedule does).
        booking.setCheckIn(LocalDate.parse(DAY).minusDays(3));
        booking.setCheckOut(LocalDate.parse(DAY).minusDays(1));
        booking = bookings.saveAndFlush(booking);
        clock.set(DAY + "T10:20");
        assertThat(reservationService.onStayChanged(booking)).isEqualTo(1);
        automation.runOnce();

        assertThat(reservations.findById(r.getId()).orElseThrow().getStatus()).isEqualTo(GymReservationStatus.CANCELLED);
        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", false);
        verify(tuya, times(1)).sendAirConditionerCommand("hub-1", "tuya-ac-1", 0, 0, 16, 3);
    }

    // ---- failures ----------------------------------------------------------------------------

    @Test
    void oneDeviceFailing_doesNotStopTheOthers_retriesThreeTimes_andIsReportedPerDevice() {
        doThrow(new TuyaException(TuyaException.Kind.NETWORK, "Tuya is not responding — try again in a minute"))
                .when(tuya).sendCommand("tuya-fan-2", "switch_1", true);
        GymReservationEntity r = reserve(booking, "10:00", 60);

        clock.set(DAY + "T09:30");
        automation.runOnce();
        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", true);
        verify(tuya, times(1)).sendAirConditionerCommand("hub-1", "tuya-ac-1", 1, 0, 16, 3);
        assertThat(onPhase(r).getState()).isEqualTo(GymAutomationPhase.StateEnum.PENDING);

        clock.set(DAY + "T09:31");
        automation.runOnce();
        clock.set(DAY + "T09:32");
        automation.runOnce();
        clock.set(DAY + "T09:33");
        automation.runOnce(); // no fourth attempt

        verify(tuya, times(3)).sendCommand("tuya-fan-2", "switch_1", true);
        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", true);
        GymAutomationPhase on = onPhase(r);
        assertThat(on.getState()).isEqualTo(GymAutomationPhase.StateEnum.PARTIAL);
        assertThat(on.getResults()).filteredOn(x -> x.getStatus().getValue().equals("FAILED"))
                .singleElement()
                .satisfies(x -> {
                    assertThat(x.getDeviceName()).isEqualTo("Fan 2");
                    assertThat(x.getError().get()).contains("not responding");
                    assertThat(x.getAttempts()).isEqualTo(3);
                });

        // The result is audited as the system, naming the device that failed.
        assertThat(auditLogRepository.findAll())
                .filteredOn(a -> a.getAction() == AuditAction.GYM_AUTOMATION_RESULT && r.getId().equals(a.getEntityId()))
                .singleElement()
                .satisfies(a -> {
                    assertThat(a.getActorUserId()).isEqualTo("SYSTEM");
                    assertThat(a.getSummary()).contains("switched on 2 of 3").contains("FAILED").contains("Fan 2");
                });
    }

    @Test
    void aFailureThatClears_succeedsOnTheRetry() {
        doThrow(new TuyaException(TuyaException.Kind.NETWORK, "Tuya is not responding — try again in a minute"))
                .doNothing()
                .when(tuya).sendCommand("tuya-fan-2", "switch_1", true);
        GymReservationEntity r = reserve(booking, "10:00", 60);

        clock.set(DAY + "T09:30");
        automation.runOnce();
        clock.set(DAY + "T09:31");
        automation.runOnce();

        verify(tuya, times(2)).sendCommand("tuya-fan-2", "switch_1", true);
        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", true);
        assertThat(onPhase(r).getState()).isEqualTo(GymAutomationPhase.StateEnum.OK);
    }

    @Test
    void aFailedSwitchOff_isRetriedToo_andReported() {
        GymReservationEntity r = reserve(booking, "10:00", 30);
        clock.set(DAY + "T09:30");
        automation.runOnce();
        doThrow(new TuyaException(TuyaException.Kind.NETWORK, "Tuya is not responding — try again in a minute"))
                .when(tuya).sendAirConditionerCommand("hub-1", "tuya-ac-1", 0, 0, 16, 3);

        clock.set(DAY + "T10:30");
        automation.runOnce();
        clock.set(DAY + "T10:31");
        automation.runOnce();
        clock.set(DAY + "T10:32");
        automation.runOnce();

        verify(tuya, times(3)).sendAirConditionerCommand("hub-1", "tuya-ac-1", 0, 0, 16, 3);
        verify(tuya, times(1)).sendCommand("tuya-fan-1", "switch_1", false);
        assertThat(offPhase(r).getState()).isEqualTo(GymAutomationPhase.StateEnum.PARTIAL);
    }

    // ---- helpers -----------------------------------------------------------------------------

    private GymAutomationService newAutomationService() {
        return new GymAutomationService(reservations, actions, devices, gymDeviceService, hallSettings, bookings,
                org.mockito.Mockito.mock(AuditLogService.class), clock, 3, 0);
    }

    private void verifyNothingPolled() {
        verify(tuya, never()).deviceState(anyString());
    }

    private GymReservationEntity reserve(BookingEntity forBooking, String startTime, int minutes) {
        GymReservationEntity e = new GymReservationEntity();
        e.setBookingId(forBooking.getId());
        e.setDate(LocalDate.parse(DAY));
        e.setStartTime(LocalTime.parse(startTime));
        e.setDurationMinutes(minutes);
        e.setCreatedByGuestAccountId(null);
        e.setCreatedByUserId(creatorUserId());
        return reservations.saveAndFlush(e);
    }

    private String creatorUserId() {
        return userId;
    }

    private String userId;

    @Autowired private com.sunsetbeach.repository.UserRepository userRepository;

    @BeforeEach
    void createStaffUser() {
        com.sunsetbeach.entity.UserEntity u = new com.sunsetbeach.entity.UserEntity();
        u.setEmail("gym-auto-" + UUID.randomUUID() + "@test.local");
        u.setName(u.getEmail());
        u.setPasswordHash("unused");
        u.setRole(com.sunsetbeach.model.Role.CASHIER);
        u.setActive(true);
        userId = userRepository.saveAndFlush(u).getId();
    }

    @AfterEach
    void deleteStaffUser() {
        reservations.deleteAll();
        if (userId != null) {
            userRepository.deleteById(userId);
        }
    }

    private GymAutomationPhase onPhase(GymReservationEntity r) {
        return GymAutomationView.phase(actions.findByReservationIdIn(List.of(r.getId())), "ON");
    }

    private GymAutomationPhase offPhase(GymReservationEntity r) {
        return GymAutomationView.phase(actions.findByReservationIdIn(List.of(r.getId())), "OFF");
    }

    private BookingEntity booking(String guest) {
        BookingEntity b = new BookingEntity();
        b.setChannel(BookingChannel.WALK_IN);
        b.setRoomId(room.getId());
        b.setGuestName(guest);
        b.setGuestPhone("+66800000000");
        b.setCheckIn(LocalDate.parse(DAY).minusDays(1));
        b.setCheckOut(LocalDate.parse(DAY).plusDays(2));
        b.setTotalPrice(new BigDecimal("1000.00"));
        b.setStatus(BookingStatus.CONFIRMED);
        b.setOccupancyStatus(OccupancyStatus.CHECKED_IN);
        return bookings.saveAndFlush(b);
    }

    private GymDeviceEntity relay(String name, String tuyaId, int order) {
        GymDeviceEntity d = new GymDeviceEntity();
        d.setName(name);
        d.setType("RELAY");
        d.setTuyaDeviceId(tuyaId);
        d.setSwitchCode("switch_1");
        d.setSortOrder(order);
        return devices.saveAndFlush(d);
    }

    private GymDeviceEntity airConditioner(String name, String tuyaId, int order) {
        GymDeviceEntity d = new GymDeviceEntity();
        d.setName(name);
        d.setType("AC_IR");
        d.setTuyaDeviceId(tuyaId);
        d.setInfraredId("hub-1");
        d.setRemoteId(tuyaId);
        d.setSortOrder(order);
        return devices.saveAndFlush(d);
    }
}
