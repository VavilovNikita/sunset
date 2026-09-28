package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.entity.GuestEmailLogEntity;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.entity.LifecycleEmailSettingsEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.GuestEmailHistoryEntry;
import com.sunsetbeach.model.LifecycleEmailType;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.repository.GuestEmailLogRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.repository.LifecycleEmailSettingsRepository;
import com.sunsetbeach.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@link LifecycleEmailService}'s sweep against the real database, with {@link EmailService}
 * mocked to report every send as successful. Today is fixed (see {@link FixedClockConfig}) and far
 * in the future, so no other test's leftover bookings land on the exact check-in/check-out dates
 * used here. Win-back candidates are global (any old stay qualifies), so those assertions are made
 * per recipient, never on the sweep's total counts.
 *
 * <p>{@code @Transactional}: the sweep itself isn't transactional, so every log row it writes
 * joins this test's transaction and rolls back with it.
 */
@SpringBootTest
@Transactional
class LifecycleEmailServiceTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.of(2041, 6, 15);
    private static final ZoneId HOTEL_ZONE = ZoneId.of("Asia/Bangkok");

    @TestConfiguration
    static class FixedClockConfig {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(TODAY.atTime(10, 0).atZone(HOTEL_ZONE).toInstant(), HOTEL_ZONE);
        }
    }

    @Autowired
    private LifecycleEmailService lifecycleEmailService;

    @Autowired
    private GuestService guestService;

    @Autowired
    private GuestAccountService guestAccountService;

    @Autowired
    private LifecycleEmailSettingsRepository settingsRepository;

    @Autowired
    private GuestEmailLogRepository guestEmailLogRepository;

    @Autowired
    private GuestRepository guestRepository;

    @Autowired
    private GuestAccountRepository guestAccountRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private EmailService emailService;

    private RoomEntity room;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Lifecycle Email Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by LifecycleEmailServiceTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);

        when(emailService.sendPreArrivalEmail(any(), any(), any(), any(), any(), any())).thenReturn(true);
        when(emailService.sendPostStayEmail(any(), any(), any(), any(), any(), any(), any())).thenReturn(true);
        when(emailService.sendWinBackEmail(any(), any(), any())).thenReturn(true);

        configure(true, 3, true, 1, "https://reviews.example.com/sunset", true, 12);
    }

    private void configure(boolean pre, int preDays, boolean post, int postDays, String reviewUrl, boolean winBack, int winBackMonths) {
        LifecycleEmailSettingsEntity settings = settingsRepository.findById(LifecycleEmailSettingsEntity.SINGLETON_ID).orElseThrow();
        settings.setPreArrivalEnabled(pre);
        settings.setPreArrivalDaysBefore(preDays);
        settings.setPostStayEnabled(post);
        settings.setPostStayDaysAfter(postDays);
        settings.setPostStayReviewUrl(reviewUrl);
        settings.setWinBackEnabled(winBack);
        settings.setWinBackMonthsSinceStay(winBackMonths);
        settingsRepository.saveAndFlush(settings);
    }

    /** A Guest card, optionally with a linked account (verified or not, opted out or not). */
    private record Fixture(GuestEntity guest, GuestAccountEntity account, String email) {
    }

    private Fixture guest(boolean withAccount, boolean verified, boolean optedOut) {
        String email = "lifecycle-" + UUID.randomUUID() + "@example.com";
        GuestEntity guest = new GuestEntity();
        guest.setName("Lifecycle Guest");
        guest.setEmail(email);
        guest = guestRepository.saveAndFlush(guest);
        GuestAccountEntity account = null;
        if (withAccount) {
            account = new GuestAccountEntity();
            account.setEmail(email);
            account.setPasswordHash("not-a-real-hash");
            account.setName("Account Name");
            account.setGuestId(guest.getId());
            account.setEmailVerifiedAt(verified ? LocalDateTime.of(2030, 1, 1, 12, 0) : null);
            account.setMarketingEmailsOptOut(optedOut);
            account = guestAccountRepository.saveAndFlush(account);
        }
        return new Fixture(guest, account, email);
    }

    private Fixture eligibleGuest() {
        return guest(true, true, false);
    }

    private BookingEntity booking(Fixture f, LocalDate checkIn, LocalDate checkOut, BookingStatus status) {
        BookingEntity booking = new BookingEntity();
        booking.setChannel(BookingChannel.DIRECT);
        booking.setRoomId(room.getId());
        booking.setGuestId(f.guest().getId());
        booking.setGuestName(f.guest().getName());
        booking.setGuestEmail("booking-snapshot@example.com");
        booking.setCheckIn(checkIn);
        booking.setCheckOut(checkOut);
        booking.setTotalPrice(new BigDecimal("1000.00"));
        booking.setStatus(status);
        booking.setSource(BookingSource.STAFF);
        return bookingRepository.saveAndFlush(booking);
    }

    private BookingEntity arrivingInThreeDays(Fixture f) {
        return booking(f, TODAY.plusDays(3), TODAY.plusDays(5), BookingStatus.CONFIRMED);
    }

    private BookingEntity departedYesterday(Fixture f) {
        return booking(f, TODAY.minusDays(3), TODAY.minusDays(1), BookingStatus.PAID);
    }

    private BookingEntity lastStayedLongAgo(Fixture f) {
        return booking(f, LocalDate.of(2040, 1, 1), LocalDate.of(2040, 1, 3), BookingStatus.PAID);
    }

    private List<GuestEmailLogEntity> logFor(Fixture f) {
        return guestEmailLogRepository.findByGuestIdOrderBySentAtDesc(f.guest().getId());
    }

    private GuestEmailLogEntity insertLog(Fixture f, String bookingId, LifecycleEmailType type, LocalDateTime sentAt) {
        GuestEmailLogEntity entry = new GuestEmailLogEntity();
        entry.setGuestId(f.guest().getId());
        entry.setBookingId(bookingId);
        entry.setType(type);
        entry.setSubject("Subject " + type);
        entry.setSentAt(sentAt);
        return guestEmailLogRepository.saveAndFlush(entry);
    }

    // --- idempotency ---

    @Test
    void preArrival_sentOnceToTheAccountsEmail_andASecondSweepTheSameDaySendsNothing() {
        Fixture f = eligibleGuest();
        BookingEntity booking = arrivingInThreeDays(f);

        lifecycleEmailService.sweep();
        lifecycleEmailService.sweep();

        // The account's verified address, not the booking's snapshot email.
        verify(emailService, times(1)).sendPreArrivalEmail(
                eq(f.email()), eq("Account Name"), eq(room.getName()), eq(TODAY.plusDays(3)), eq(TODAY.plusDays(5)),
                eq(f.account().getUnsubscribeToken()));
        assertThat(logFor(f)).singleElement().satisfies(entry -> {
            assertThat(entry.getType()).isEqualTo(LifecycleEmailType.PRE_ARRIVAL);
            assertThat(entry.getBookingId()).isEqualTo(booking.getId());
            assertThat(entry.getSubject()).isEqualTo(EmailService.PRE_ARRIVAL_SUBJECT);
        });
    }

    @Test
    void postStay_sentOnceWithTheReviewUrl_andASecondSweepSendsNothing() {
        Fixture f = eligibleGuest();
        departedYesterday(f);

        lifecycleEmailService.sweep();
        lifecycleEmailService.sweep();

        verify(emailService, times(1)).sendPostStayEmail(
                eq(f.email()), any(), any(), any(), any(), eq("https://reviews.example.com/sunset"), any());
        assertThat(logFor(f)).extracting(GuestEmailLogEntity::getType).containsExactly(LifecycleEmailType.POST_STAY);
    }

    @Test
    void postStay_withNoReviewUrl_isStillSent_withoutOne() {
        configure(false, 3, true, 1, null, false, 12);
        Fixture f = eligibleGuest();
        departedYesterday(f);

        lifecycleEmailService.sweep();

        verify(emailService).sendPostStayEmail(eq(f.email()), any(), any(), any(), any(), isNull(), any());
    }

    @Test
    void winBack_sentOnce_andASecondSweepSendsNothing() {
        Fixture f = eligibleGuest();
        lastStayedLongAgo(f);

        lifecycleEmailService.sweep();
        lifecycleEmailService.sweep();

        verify(emailService, times(1)).sendWinBackEmail(f.email(), "Account Name", f.account().getUnsubscribeToken());
        assertThat(logFor(f)).singleElement().satisfies(entry -> {
            assertThat(entry.getType()).isEqualTo(LifecycleEmailType.WIN_BACK);
            assertThat(entry.getBookingId()).isNull();
        });
    }

    // --- win-back eligibility ---

    @Test
    void winBack_notSentWhileTheLastStayIsRecent_orAnotherStayIsUpcoming() {
        Fixture recent = eligibleGuest();
        booking(recent, TODAY.minusMonths(6), TODAY.minusMonths(6).plusDays(2), BookingStatus.PAID);
        Fixture returning = eligibleGuest();
        lastStayedLongAgo(returning);
        booking(returning, TODAY.plusMonths(2), TODAY.plusMonths(2).plusDays(2), BookingStatus.CONFIRMED);

        lifecycleEmailService.sweep();

        verify(emailService, never()).sendWinBackEmail(eq(recent.email()), any(), any());
        verify(emailService, never()).sendWinBackEmail(eq(returning.email()), any(), any());
    }

    @Test
    void winBack_aCancelledBookingDoesNotCountAsTheLastStay() {
        Fixture f = eligibleGuest();
        lastStayedLongAgo(f);
        booking(f, TODAY.minusMonths(1), TODAY.minusMonths(1).plusDays(2), BookingStatus.CANCELLED);

        lifecycleEmailService.sweep();

        verify(emailService).sendWinBackEmail(eq(f.email()), any(), any());
    }

    @Test
    void winBack_repeatsOnlyOnceTheLastWinBackIsOlderThanTheSameWindow() {
        Fixture nudgedRecently = eligibleGuest();
        lastStayedLongAgo(nudgedRecently);
        insertLog(nudgedRecently, null, LifecycleEmailType.WIN_BACK, TODAY.minusMonths(3).atStartOfDay());
        Fixture nudgedLongAgo = eligibleGuest();
        lastStayedLongAgo(nudgedLongAgo);
        insertLog(nudgedLongAgo, null, LifecycleEmailType.WIN_BACK, TODAY.minusMonths(13).atStartOfDay());

        lifecycleEmailService.sweep();

        verify(emailService, never()).sendWinBackEmail(eq(nudgedRecently.email()), any(), any());
        verify(emailService, times(1)).sendWinBackEmail(eq(nudgedLongAgo.email()), any(), any());
    }

    // --- audience ---

    @Test
    void optedOutGuest_getsNoneOfTheThree_evenWhenEligibleForEach() {
        Fixture preArrival = guest(true, true, true);
        arrivingInThreeDays(preArrival);
        Fixture postStay = guest(true, true, true);
        departedYesterday(postStay);
        Fixture winBack = guest(true, true, true);
        lastStayedLongAgo(winBack);

        lifecycleEmailService.sweep();

        for (Fixture f : List.of(preArrival, postStay, winBack)) {
            verify(emailService, never()).sendPreArrivalEmail(eq(f.email()), any(), any(), any(), any(), any());
            verify(emailService, never()).sendPostStayEmail(eq(f.email()), any(), any(), any(), any(), any(), any());
            verify(emailService, never()).sendWinBackEmail(eq(f.email()), any(), any());
            assertThat(logFor(f)).isEmpty();
        }
    }

    @Test
    void guestWithNoAccount_orAnUnverifiedOne_neverReceivesAnything() {
        Fixture noAccount = guest(false, false, false);
        Fixture unverified = guest(true, false, false);
        for (Fixture f : List.of(noAccount, unverified)) {
            arrivingInThreeDays(f);
            departedYesterday(f);
        }
        Fixture noAccountOld = guest(false, false, false);
        lastStayedLongAgo(noAccountOld);
        Fixture unverifiedOld = guest(true, false, false);
        lastStayedLongAgo(unverifiedOld);

        lifecycleEmailService.sweep();

        for (Fixture f : List.of(noAccount, unverified, noAccountOld, unverifiedOld)) {
            verify(emailService, never()).sendPreArrivalEmail(eq(f.email()), any(), any(), any(), any(), any());
            verify(emailService, never()).sendPostStayEmail(eq(f.email()), any(), any(), any(), any(), any(), any());
            verify(emailService, never()).sendWinBackEmail(eq(f.email()), any(), any());
            assertThat(logFor(f)).isEmpty();
        }
    }

    @Test
    void unsubscribe_byToken_stopsEveryType_withNoLogin() {
        Fixture f = eligibleGuest();
        arrivingInThreeDays(f);

        guestAccountService.unsubscribe(f.account().getUnsubscribeToken());
        // Idempotent - a second click on the same link is fine.
        guestAccountService.unsubscribe(f.account().getUnsubscribeToken());
        lifecycleEmailService.sweep();

        assertThat(guestAccountRepository.findById(f.account().getId()).orElseThrow().isMarketingEmailsOptOut()).isTrue();
        verify(emailService, never()).sendPreArrivalEmail(eq(f.email()), any(), any(), any(), any(), any());
    }

    @Test
    void unsubscribe_unknownToken_isBadRequest() {
        assertThatThrownBy(() -> guestAccountService.unsubscribe("no-such-token")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void everyNewAccount_getsAnUnsubscribeToken() {
        Fixture a = eligibleGuest();
        Fixture b = eligibleGuest();

        assertThat(a.account().getUnsubscribeToken()).isNotBlank().isNotEqualTo(b.account().getUnsubscribeToken());
    }

    // --- booking status / occupancy ---

    @Test
    void cancelledBookings_andNoShows_getNoStayEmail() {
        Fixture cancelledArrival = eligibleGuest();
        booking(cancelledArrival, TODAY.plusDays(3), TODAY.plusDays(5), BookingStatus.CANCELLED);
        Fixture cancelledDeparture = eligibleGuest();
        booking(cancelledDeparture, TODAY.minusDays(3), TODAY.minusDays(1), BookingStatus.CANCELLED);
        Fixture noShow = eligibleGuest();
        BookingEntity noShowBooking = departedYesterday(noShow);
        noShowBooking.setOccupancyStatus(OccupancyStatus.NO_SHOW);
        bookingRepository.saveAndFlush(noShowBooking);

        lifecycleEmailService.sweep();

        verify(emailService, never()).sendPreArrivalEmail(eq(cancelledArrival.email()), any(), any(), any(), any(), any());
        verify(emailService, never()).sendPostStayEmail(eq(cancelledDeparture.email()), any(), any(), any(), any(), any(), any());
        verify(emailService, never()).sendPostStayEmail(eq(noShow.email()), any(), any(), any(), any(), any(), any());
    }

    @Test
    void onlyTheExactConfiguredDay_matches() {
        Fixture tooEarly = eligibleGuest();
        booking(tooEarly, TODAY.plusDays(4), TODAY.plusDays(6), BookingStatus.CONFIRMED);
        Fixture tooLate = eligibleGuest();
        booking(tooLate, TODAY.minusDays(4), TODAY.minusDays(2), BookingStatus.PAID);

        lifecycleEmailService.sweep();

        verify(emailService, never()).sendPreArrivalEmail(eq(tooEarly.email()), any(), any(), any(), any(), any());
        verify(emailService, never()).sendPostStayEmail(eq(tooLate.email()), any(), any(), any(), any(), any(), any());
    }

    // --- settings ---

    @Test
    void disablingOneType_stopsOnlyThatType() {
        configure(true, 3, false, 1, null, true, 12);
        Fixture f = eligibleGuest();
        arrivingInThreeDays(f);
        departedYesterday(f);
        Fixture old = eligibleGuest();
        lastStayedLongAgo(old);

        lifecycleEmailService.sweep();

        verify(emailService).sendPreArrivalEmail(eq(f.email()), any(), any(), any(), any(), any());
        verify(emailService, never()).sendPostStayEmail(any(), any(), any(), any(), any(), any(), any());
        verify(emailService).sendWinBackEmail(eq(old.email()), any(), any());
    }

    @Test
    void thresholdsComeFromSettings() {
        configure(true, 7, true, 2, null, false, 12);
        Fixture f = eligibleGuest();
        booking(f, TODAY.plusDays(7), TODAY.plusDays(9), BookingStatus.CONFIRMED);
        booking(f, TODAY.minusDays(5), TODAY.minusDays(2), BookingStatus.PAID);

        lifecycleEmailService.sweep();

        verify(emailService).sendPreArrivalEmail(eq(f.email()), any(), any(), eq(TODAY.plusDays(7)), any(), any());
        verify(emailService).sendPostStayEmail(eq(f.email()), any(), any(), any(), eq(TODAY.minusDays(2)), any(), any());
    }

    // --- log reflects what actually went out ---

    @Test
    void aFailedSend_writesNoLogRow_andDoesNotStopTheRest() {
        Fixture failing = eligibleGuest();
        arrivingInThreeDays(failing);
        Fixture throwing = eligibleGuest();
        arrivingInThreeDays(throwing);
        Fixture fine = eligibleGuest();
        arrivingInThreeDays(fine);
        when(emailService.sendPreArrivalEmail(eq(failing.email()), any(), any(), any(), any(), any())).thenReturn(false);
        when(emailService.sendPreArrivalEmail(eq(throwing.email()), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("boom"));

        lifecycleEmailService.sweep();

        assertThat(logFor(failing)).isEmpty();
        assertThat(logFor(throwing)).isEmpty();
        assertThat(logFor(fine)).hasSize(1);
    }

    // --- guest card history ---

    @Test
    void guestDetailEmailHistory_isExactlyWhatWasLogged_newestFirst() {
        Fixture f = eligibleGuest();
        BookingEntity stay = lastStayedLongAgo(f);
        insertLog(f, stay.getId(), LifecycleEmailType.PRE_ARRIVAL, LocalDateTime.of(2039, 12, 29, 3, 0));
        insertLog(f, stay.getId(), LifecycleEmailType.POST_STAY, LocalDateTime.of(2040, 1, 4, 3, 0));
        Fixture other = eligibleGuest();
        insertLog(other, null, LifecycleEmailType.WIN_BACK, LocalDateTime.of(2040, 1, 5, 3, 0));

        // Adds a WIN_BACK row for f, stamped from the fixed Clock - the newest of the three.
        lifecycleEmailService.sweep();
        // Reload from the database: the bookings saved above are still managed with their lazy
        // room association unset, which GuestService's booking mapping reads.
        entityManager.flush();
        entityManager.clear();

        List<GuestEmailHistoryEntry> history = guestService.getDetail(f.guest().getId()).getEmailHistory();
        assertThat(history).extracting(GuestEmailHistoryEntry::getType)
                .containsExactly(LifecycleEmailType.WIN_BACK, LifecycleEmailType.POST_STAY, LifecycleEmailType.PRE_ARRIVAL);
        assertThat(history.get(0).getBookingId().orElse(null)).isNull();
        assertThat(history.get(0).getSubject()).isEqualTo(EmailService.WIN_BACK_SUBJECT);
        // Hotel wall-clock, zoned as such - not relabelled as UTC.
        assertThat(history.get(0).getSentAt()).isEqualTo(TODAY.atTime(10, 0).atZone(HOTEL_ZONE).toOffsetDateTime());
        assertThat(history.get(1).getBookingId().orElse(null)).isEqualTo(stay.getId());
        assertThat(history.get(2).getSentAt()).isEqualTo(LocalDateTime.of(2039, 12, 29, 3, 0).atZone(HOTEL_ZONE).toOffsetDateTime());
    }
}
