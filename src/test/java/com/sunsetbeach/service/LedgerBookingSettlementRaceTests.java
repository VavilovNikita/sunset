package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.FolioPaymentInput;
import com.sunsetbeach.model.FolioPaymentMethod;
import com.sunsetbeach.model.JournalSourceType;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.JournalEntryRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Two concurrent "mark PAID" requests for one booking must post one room settlement, not two -
 * {@code BookingService#updateStatus} row-locks the booking so the second request sees the first
 * one's PAID and posts nothing. Without the lock both read CONFIRMED and the ledger records the
 * room as collected twice. Both requests carry the payment the desk was shown; only the first one
 * is recorded (the second finds the booking already paid), so the guest is not charged twice.
 *
 * <p>Deliberately not {@code @Transactional}: the race needs two real, separate transactions.
 * Cleans up what it wrote in {@link #cleanUp()} instead.
 */
@SpringBootTest
class LedgerBookingSettlementRaceTests extends AbstractIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private JournalEntryRepository entryRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private UserRepository userRepository;

    private String roomId;
    private String bookingId;
    private UserEntity user;

    @AfterEach
    void cleanUp() {
        if (bookingId != null) {
            String payments = "(SELECT id FROM \"FolioPayment\" WHERE \"bookingId\" = ?)";
            jdbcTemplate.update("DELETE FROM \"JournalLine\" WHERE \"entryId\" IN (SELECT id FROM \"JournalEntry\" WHERE \"sourceId\" IN " + payments + ")", bookingId);
            jdbcTemplate.update("DELETE FROM \"JournalEntry\" WHERE \"sourceId\" IN " + payments, bookingId);
            jdbcTemplate.update("DELETE FROM \"FolioPayment\" WHERE \"bookingId\" = ?", bookingId);
            jdbcTemplate.update("DELETE FROM \"JournalLine\" WHERE \"entryId\" IN (SELECT id FROM \"JournalEntry\" WHERE \"sourceId\" = ?)", bookingId);
            jdbcTemplate.update("DELETE FROM \"JournalEntry\" WHERE \"sourceId\" = ? AND \"reversesEntryId\" IS NOT NULL", bookingId);
            jdbcTemplate.update("DELETE FROM \"JournalEntry\" WHERE \"sourceId\" = ?", bookingId);
            jdbcTemplate.update("DELETE FROM \"AuditLog\" WHERE \"entityId\" = ?", bookingId);
            bookingRepository.deleteById(bookingId);
        }
        if (roomId != null) {
            roomRepository.deleteById(roomId);
        }
        if (user != null) {
            userRepository.deleteById(user.getId());
        }
        SecurityContextHolder.clearContext();
    }

    @Test
    void concurrentMarkPaid_postsExactlyOneSettlement() throws Exception {
        RoomEntity room = new RoomEntity();
        room.setName("Ledger Race Room " + UUID.randomUUID());
        room.setDescription("Room used only by LedgerBookingSettlementRaceTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        roomId = roomRepository.saveAndFlush(room).getId();

        BookingEntity booking = new BookingEntity();
        booking.setChannel(BookingChannel.DIRECT);
        booking.setRoomId(roomId);
        booking.setGuestName("Race Guest");
        booking.setGuestEmail("race@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.of(2034, 4, 1));
        booking.setCheckOut(LocalDate.of(2034, 4, 3));
        booking.setTotalPrice(new BigDecimal("1070.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingId = bookingRepository.saveAndFlush(booking).getId();

        UserEntity newUser = new UserEntity();
        newUser.setEmail("ledger-race-" + UUID.randomUUID() + "@example.com");
        newUser.setName(newUser.getEmail());
        newUser.setPasswordHash("irrelevant-for-this-test");
        newUser.setRole(Role.CASHIER);
        user = userRepository.saveAndFlush(newUser);
        FolioPaymentInput payment = new FolioPaymentInput(FolioPaymentMethod.CASH, "1070.00");

        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<?>> futures = List.of(
                    executor.submit(() -> {
                        signIn();
                        barrier.await();
                        return bookingService.updateStatus(bookingId, new BookingStatusInput(BookingStatus.PAID).payment(payment));
                    }),
                    executor.submit(() -> {
                        signIn();
                        barrier.await();
                        return bookingService.updateStatus(bookingId, new BookingStatusInput(BookingStatus.PAID).payment(payment));
                    }));
            for (Future<?> future : futures) {
                future.get();
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(entryRepository.findBySourceTypeAndSourceIdOrderByCreatedAtAsc(JournalSourceType.BOOKING, bookingId)).hasSize(1);
        assertThat(bookingService.listFolioPayments(bookingId)).hasSize(1);
    }

    /** A pool thread has no security context of its own; the payment needs the recorder. */
    private void signIn() {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                new StaffPrincipal(user.getId(), user.getEmail(), Role.CASHIER), null, List.of()));
    }
}
