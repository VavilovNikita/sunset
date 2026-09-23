package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.AuditLogEntity;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.repository.AuditLogRepository;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * DB-backed (real dev Postgres), NOT {@code @Transactional} - see {@link BookingAuditLogTests}'s
 * javadoc for why (AuditLogService.record/recordSystemAction both commit independently of this
 * test's transaction).
 *
 * <p>Specifically exercises {@link AuditLogService#recordSystemAction} via
 * {@link BookingExpiryService}'s scheduled sweep - the one call site in this codebase that runs
 * with no authenticated {@code StaffPrincipal} at all, which is exactly the case {@code record}
 * cannot handle (see both methods' own javadoc).
 */
@SpringBootTest
class BookingExpirySystemAuditLogTests extends AbstractIntegrationTest {

    @Autowired
    private BookingExpiryService bookingExpiryService;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<String> createdRoomIds = new java.util.ArrayList<>();
    private final List<String> createdBookingIds = new java.util.ArrayList<>();

    @BeforeEach
    void clearSecurityContext() {
        // No authenticated principal for the whole test - this is what actually happens on the
        // @Scheduled thread this sweep runs on in production; a stray leftover context from an
        // earlier test would defeat the point of this test class.
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        for (String bookingId : createdBookingIds) {
            auditLogRepository.deleteAll(entriesFor(bookingId));
            bookingRepository.deleteById(bookingId);
        }
        for (String roomId : createdRoomIds) {
            roomRepository.deleteById(roomId);
        }
    }

    private List<AuditLogEntity> entriesFor(String bookingId) {
        return auditLogRepository.findAll().stream()
                .filter(e -> e.getEntityType() == AuditEntityType.BOOKING && bookingId.equals(e.getEntityId()))
                .toList();
    }

    private BookingEntity persistStalePublicBooking(RoomEntity room) {
        BookingEntity booking = new BookingEntity();
        booking.setRoomId(room.getId());
        booking.setGuestName("System Audit Guest");
        booking.setGuestEmail("system-audit-guest@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.now().plusDays(30));
        booking.setCheckOut(LocalDate.now().plusDays(31));
        booking.setTotalPrice(new BigDecimal("1000.00"));
        booking.setStatus(BookingStatus.NEW);
        booking.setSource(BookingSource.PUBLIC);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        // Same backdating idea as BookingExpiryServiceTests#persistBooking - @CreationTimestamp is
        // an insert-time value generator, so it needs a raw UPDATE, not a managed-entity field
        // set. Unlike that class (which is @Transactional), this test class deliberately has no
        // active transaction on its own thread - a JPA entityManager native query would fail with
        // "No active transaction" - so this goes through a plain auto-committing JdbcTemplate
        // statement instead, which needs no transaction and leaves no first-level-cache entry to
        // go stale (there's no persistence context here to begin with).
        jdbcTemplate.update("UPDATE \"Booking\" SET \"createdAt\" = ? WHERE id = ?", Timestamp.valueOf(LocalDateTime.now().minusDays(10)), saved.getId());
        return saved;
    }

    @Test
    void autoCancelWithNoAuthenticatedPrincipal_writesSystemAuditEntry() {
        RoomEntity room = new RoomEntity();
        room.setName("System Audit Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by BookingExpirySystemAuditLogTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);
        createdRoomIds.add(savedRoom.getId());

        BookingEntity stale = persistStalePublicBooking(savedRoom);
        createdBookingIds.add(stale.getId());

        bookingExpiryService.sweepUnconfirmedPublicBookings();

        List<AuditLogEntity> entries = entriesFor(stale.getId()).stream()
                .filter(e -> e.getAction() == AuditAction.BOOKING_STATUS_CHANGED)
                .toList();
        assertThat(entries).hasSize(1);
        AuditLogEntity entry = entries.get(0);
        assertThat(entry.getActorUserId()).isEqualTo("SYSTEM");
        assertThat(entry.getActorEmail()).isEqualTo("system@sunsetbeach.internal");
        assertThat(entry.getActorRole()).isNull();
        assertThat(entry.getSummary()).contains("NEW").contains("CANCELLED").contains("auto-cancelled");
    }
}
