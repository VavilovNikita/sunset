package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): covers the auto-cancellation of
 * stale, unconfirmed public bookings - the real bound on flooding POST /bookings with fake
 * reservations to hold the inventory hostage - and the business-day-aware staff reminder that's
 * meant to make the actual cancellation a rare event (see BookingExpiryService's javadoc).
 *
 * <p>The "N business days elapsed" boundary itself is exercised precisely and deterministically
 * (fixed reference dates, not relative to whatever day the suite happens to run) in
 * {@link BusinessDayCounterTests}; this class only needs to prove the sweep's wiring - fetch,
 * decide, act - is correct, so it uses generously-clear-of-any-boundary ages ("just now" vs. "10
 * calendar days ago", which contains far more than 2 weekdays under any possible calendar
 * alignment) plus one property-overridden case to exercise the reminder deterministically without
 * needing to hit an exact business-day boundary against the real clock.
 */
@SpringBootTest
@Transactional
class BookingExpiryServiceTests extends AbstractIntegrationTest {

    @Autowired
    private BookingExpiryService bookingExpiryService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoSpyBean
    private EmailService emailService;

    private RoomEntity room;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        // Overridden per-test where a specific boundary is needed (see
        // reminderFiresOnce_withOneBusinessDayThreshold below) - this default just documents that
        // the property exists and is read at all.
        registry.add("app.booking.new-booking-expiry-business-days", () -> "2");
    }

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Expiry Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by BookingExpiryServiceTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);
    }

    private BookingEntity persistBooking(BookingSource source, LocalDateTime createdAt) {
        BookingEntity booking = new BookingEntity();
        booking.setRoomId(room.getId());
        booking.setGuestName("Guest");
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.now().plusDays(30));
        booking.setCheckOut(LocalDate.now().plusDays(31));
        booking.setTotalPrice(new BigDecimal("1000.00"));
        booking.setStatus(BookingStatus.NEW);
        booking.setSource(source);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        // @CreationTimestamp is a Hibernate insert-time value generator - mutating the managed
        // entity's field directly (e.g. via ReflectionTestUtils) is not picked up by dirty
        // checking for a generated property, so backdating it requires bypassing the entity
        // lifecycle with a native UPDATE, then clearing the persistence context so the next read
        // reloads the row from the DB instead of returning the still-cached (non-backdated) instance.
        entityManager
                .createNativeQuery("UPDATE \"Booking\" SET \"createdAt\" = ?1 WHERE id = ?2")
                .setParameter(1, createdAt)
                .setParameter(2, saved.getId())
                .executeUpdate();
        entityManager.clear();
        return saved;
    }

    @Test
    void clearlyStalePublicBooking_isCancelledWithoutGuestEmail() {
        // 10 calendar days ago contains at least 7 weekdays under any possible alignment - far
        // past the default 2-business-day threshold regardless of which day this test runs on.
        BookingEntity stale = persistBooking(BookingSource.PUBLIC, LocalDateTime.now().minusDays(10));

        bookingExpiryService.sweepUnconfirmedPublicBookings();

        BookingEntity reloaded = bookingRepository.findById(stale.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.CANCELLED);
        // The whole point of the reminder-first design: an auto-cancellation must never trigger
        // the guest-facing "your booking has been cancelled" email - only a human decision via
        // BookingService.updateStatus does that.
        verify(emailService, never()).sendGuestStatusEmail(any(), any());
    }

    @Test
    void freshPublicBooking_isNeitherRemindedNorCancelled() {
        BookingEntity fresh = persistBooking(BookingSource.PUBLIC, LocalDateTime.now());

        bookingExpiryService.sweepUnconfirmedPublicBookings();

        BookingEntity reloaded = bookingRepository.findById(fresh.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.NEW);
        assertThat(reloaded.isExpiryReminderSent()).isFalse();
        verify(emailService, never()).sendBookingExpiringReminderDigestEmail(any(), any());
    }

    @Test
    void staleStaffBooking_isNeverTouched() {
        // A front-desk booking is confirmed by a human at creation time - the sweep must not
        // touch it, or even consider it for a reminder, no matter how old it is.
        BookingEntity staleStaffBooking = persistBooking(BookingSource.STAFF, LocalDateTime.now().minusDays(10));

        bookingExpiryService.sweepUnconfirmedPublicBookings();

        BookingEntity reloaded = bookingRepository.findById(staleStaffBooking.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.NEW);
        assertThat(reloaded.isExpiryReminderSent()).isFalse();
        verify(emailService, never()).sendBookingExpiringReminderDigestEmail(any(), any());
    }

    // The reminder-fires-once-then-stops behavior needs a controlled threshold to test without
    // depending on which real weekday the suite happens to run on - see
    // BookingExpiryServiceReminderTests, which boots its own context with
    // app.booking.new-booking-expiry-business-days overridden to 1.
}
