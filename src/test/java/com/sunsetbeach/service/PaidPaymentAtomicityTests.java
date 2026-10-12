package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.FolioPaymentInput;
import com.sunsetbeach.model.FolioPaymentMethod;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * "The payment and the status change are one transaction": a request that records the payment and
 * then fails later in the same call (here: a payment note that looks like a card number) leaves
 * neither behind - no folio payment, no ledger entry, and the booking is not PAID.
 *
 * <p>Deliberately not {@code @Transactional}: a test-wide transaction would swallow the very rollback
 * under test. Cleans up what it wrote in {@link #cleanUp()}.
 */
@SpringBootTest
class PaidPaymentAtomicityTests extends AbstractIntegrationTest {

    @Autowired private BookingService bookingService;
    @Autowired private BookingRepository bookingRepository;
    @Autowired private RoomRepository roomRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private JdbcTemplate jdbcTemplate;

    private String roomId;
    private String bookingId;
    private String userId;

    @AfterEach
    void cleanUp() {
        SecurityContextHolder.clearContext();
        if (bookingId != null) {
            jdbcTemplate.update("DELETE FROM \"FolioPayment\" WHERE \"bookingId\" = ?", bookingId);
            jdbcTemplate.update("DELETE FROM \"AuditLog\" WHERE \"entityId\" = ?", bookingId);
            bookingRepository.deleteById(bookingId);
        }
        if (roomId != null) {
            roomRepository.deleteById(roomId);
        }
        if (userId != null) {
            userRepository.deleteById(userId);
        }
    }

    @Test
    void aFailureAfterThePaymentWasRecorded_rollsTheWholeRequestBack() {
        RoomEntity room = new RoomEntity();
        room.setName("Atomicity Room " + UUID.randomUUID());
        room.setDescription("Room used only by PaidPaymentAtomicityTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        roomId = roomRepository.saveAndFlush(room).getId();

        BookingEntity booking = new BookingEntity();
        booking.setChannel(BookingChannel.DIRECT);
        booking.setRoomId(roomId);
        booking.setGuestName("Atomic Guest");
        booking.setGuestEmail("atomic@example.com");
        booking.setGuestPhone("+66800000000");
        booking.setCheckIn(LocalDate.of(2034, 6, 1));
        booking.setCheckOut(LocalDate.of(2034, 6, 3));
        booking.setTotalPrice(new BigDecimal("2000.00"));
        booking.setStatus(BookingStatus.CONFIRMED);
        bookingId = bookingRepository.saveAndFlush(booking).getId();

        UserEntity user = new UserEntity();
        user.setEmail("atomicity-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash("irrelevant-for-this-test");
        user.setRole(Role.CASHIER);
        user = userRepository.saveAndFlush(user);
        userId = user.getId();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(new StaffPrincipal(user.getId(), user.getEmail(), Role.CASHIER), null, List.of()));

        BookingStatusInput request = new BookingStatusInput(BookingStatus.PAID)
                .payment(new FolioPaymentInput(FolioPaymentMethod.CASH, "2000.00"))
                .paymentNote("4111 1111 1111 1111");

        assertThatThrownBy(() -> bookingService.updateStatus(bookingId, request)).isInstanceOf(ValidationException.class);

        assertThat(bookingService.listFolioPayments(bookingId)).isEmpty();
        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONFIRMED);
        Integer ledgerEntries = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM \"JournalEntry\" WHERE \"sourceId\" = ?", Integer.class, bookingId);
        assertThat(ledgerEntries).isZero();
    }
}
