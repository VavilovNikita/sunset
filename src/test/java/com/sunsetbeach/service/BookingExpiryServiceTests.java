package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed (real dev Postgres, rolled back after each test): covers the auto-cancellation of
 * stale, unconfirmed public bookings - the real bound on flooding POST /bookings with fake
 * reservations to hold the inventory hostage (see BookingExpiryService's javadoc). Uses the
 * configured default (24h) via app.security's test properties inherited from the app context;
 * dates here are picked well clear of that threshold in either direction so the test doesn't
 * become flaky if the default is ever tuned slightly.
 */
@SpringBootTest
@Transactional
class BookingExpiryServiceTests {

    @Autowired
    private BookingExpiryService bookingExpiryService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private EntityManager entityManager;

    private RoomEntity room;

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
    void stalePublicBooking_isCancelled() {
        BookingEntity stale = persistBooking(BookingSource.PUBLIC, LocalDateTime.now().minusHours(48));

        bookingExpiryService.cancelExpiredPublicBookings();

        BookingEntity reloaded = bookingRepository.findById(stale.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.CANCELLED);
    }

    @Test
    void recentPublicBooking_isNotCancelled() {
        BookingEntity fresh = persistBooking(BookingSource.PUBLIC, LocalDateTime.now().minusHours(1));

        bookingExpiryService.cancelExpiredPublicBookings();

        BookingEntity reloaded = bookingRepository.findById(fresh.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.NEW);
    }

    @Test
    void staleStaffBooking_isNeverCancelled() {
        // A front-desk booking is confirmed by a human at creation time - the sweep must not
        // touch it no matter how old it is.
        BookingEntity staleStaffBooking = persistBooking(BookingSource.STAFF, LocalDateTime.now().minusHours(48));

        bookingExpiryService.cancelExpiredPublicBookings();

        BookingEntity reloaded = bookingRepository.findById(staleStaffBooking.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(BookingStatus.NEW);
    }
}
