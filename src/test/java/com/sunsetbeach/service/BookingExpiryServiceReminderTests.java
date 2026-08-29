package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import java.math.BigDecimal;
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
 * A separate Spring context (via {@code @DynamicPropertySource}, which Spring boots once per
 * distinct property set and caches) from {@link BookingExpiryServiceTests}, specifically to pin
 * {@code app.booking.new-booking-expiry-business-days} to 1 - {@code @Value} is resolved once at
 * bean construction, so this can't be varied per-test-method within a single context. With the
 * threshold at 1, a booking created "just now" (0 business days waiting) already satisfies the
 * reminder condition (0 &gt;= threshold-1 == 0) without needing to land on an exact business-day
 * boundary against the real calendar - the source of the flakiness this split avoids.
 */
@SpringBootTest
@Transactional
class BookingExpiryServiceReminderTests {

    @Autowired
    private BookingExpiryService bookingExpiryService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @MockitoSpyBean
    private EmailService emailService;

    private RoomEntity room;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.booking.new-booking-expiry-business-days", () -> "1");
    }

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Expiry Reminder Test Room " + UUID.randomUUID());
        newRoom.setDescription("Room used only by BookingExpiryServiceReminderTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);
    }

    @Test
    void reminderFiresOnce_thenIsNotRepeatedOnSubsequentSweeps_andNeverCancelsAtThisPoint() {
        BookingEntity booking = new BookingEntity();
        booking.setRoomId(room.getId());
        booking.setGuestName("Guest");
        booking.setGuestEmail("guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(java.time.LocalDate.now().plusDays(30));
        booking.setCheckOut(java.time.LocalDate.now().plusDays(31));
        booking.setTotalPrice(new BigDecimal("1000.00"));
        booking.setStatus(BookingStatus.NEW);
        booking.setSource(BookingSource.PUBLIC);
        booking = bookingRepository.saveAndFlush(booking);

        bookingExpiryService.sweepUnconfirmedPublicBookings();

        BookingEntity afterFirstSweep = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(afterFirstSweep.getStatus()).isEqualTo(BookingStatus.NEW);
        assertThat(afterFirstSweep.isExpiryReminderSent()).isTrue();
        verify(emailService, times(1)).sendBookingExpiringReminderEmail(any(), any());

        bookingExpiryService.sweepUnconfirmedPublicBookings();

        BookingEntity afterSecondSweep = bookingRepository.findById(booking.getId()).orElseThrow();
        // 0 business days have elapsed (created moments ago) and the threshold is 1, so this
        // booking is still NEW - only the reminder condition (>= threshold - 1 == 0) is met.
        assertThat(afterSecondSweep.getStatus()).isEqualTo(BookingStatus.NEW);
        verify(emailService, times(1)).sendBookingExpiringReminderEmail(any(), any());
    }
}
