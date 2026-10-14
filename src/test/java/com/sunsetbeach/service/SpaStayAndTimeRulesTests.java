package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.entity.TableEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.CheckOutInput;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.MenuDepartment;
import com.sunsetbeach.model.Order;
import com.sunsetbeach.model.OrderCreateInput;
import com.sunsetbeach.model.OrderItemInput;
import com.sunsetbeach.model.OrderStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.SpaAppointment;
import com.sunsetbeach.model.SpaAppointmentCreateInput;
import com.sunsetbeach.model.SpaAppointmentScheduleInput;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.Zone;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.TableRepository;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import com.sunsetbeach.support.FixedHotelClockConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * When the spa can be booked, and what a shorter stay does to it (owner's decisions after the
 * second QA round): a treatment's date is always inside the stay - for every kind of booking,
 * a guest in the house included - and never starts before the slot that is under way; and a
 * stay that gets shorter (a schedule edit, an early check-out) cancels the guest's still-ahead
 * {@code BOOKED} appointments outside the new dates, unless one of them is already paid. "Now" is
 * pinned to 2034-03-10 12:20, Bangkok (FixedHotelClockConfig) - the 12:00 slot is the current one.
 */
@SpringBootTest
@Import(FixedHotelClockConfig.class)
@Transactional
class SpaStayAndTimeRulesTests extends AbstractIntegrationTest {

    private static final LocalDate TODAY = LocalDate.parse("2034-03-10");

    @Autowired private SpaAppointmentService spaAppointmentService;
    @Autowired private BookingService bookingService;
    @Autowired private BookingOccupancyService occupancyService;
    @Autowired private OrderService orderService;
    @Autowired private RoomRepository roomRepository;
    @Autowired private RoomUnitRepository roomUnitRepository;
    @Autowired private TableRepository tableRepository;
    @Autowired private MenuItemRepository menuItemRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SpaAppointmentRepository spaAppointmentRepository;
    @Autowired private OrderRepository orderRepository;

    private RoomEntity room;
    private RoomUnitEntity unit;
    private TableEntity spaTable;
    private UserEntity therapist;
    private UserEntity receptionist;
    private MenuItemEntity massage;

    @BeforeEach
    void setUp() {
        RoomEntity newRoom = new RoomEntity();
        newRoom.setName("Spa Time Room " + UUID.randomUUID());
        newRoom.setDescription("Used only by SpaStayAndTimeRulesTests");
        newRoom.setCapacity(2);
        newRoom.setBasePrice(new BigDecimal("1000.00"));
        room = roomRepository.saveAndFlush(newRoom);
        RoomUnitEntity newUnit = new RoomUnitEntity();
        newUnit.setRoomId(room.getId());
        newUnit.setLabel("SpaTime-" + UUID.randomUUID().toString().substring(0, 8));
        newUnit.setActive(true);
        unit = roomUnitRepository.saveAndFlush(newUnit);

        TableEntity table = new TableEntity();
        table.setZone(Zone.SPA);
        table.setLabel("Spa Time Table " + UUID.randomUUID());
        table.setCapacity(2);
        table.setActive(true);
        spaTable = tableRepository.saveAndFlush(table);
        therapist = user(Role.WAITER, JobFunction.THERAPIST.getValue());
        receptionist = user(Role.CASHIER);

        MenuItemEntity item = new MenuItemEntity();
        item.setName("Massage " + UUID.randomUUID());
        item.setDescription("Used only by SpaStayAndTimeRulesTests");
        item.setCategory("Massage");
        item.setDepartment(MenuDepartment.SPA);
        item.setPrice(new BigDecimal("1500.00"));
        item.setAvailable(true);
        item.setDurationMinutes(60);
        massage = menuItemRepository.saveAndFlush(item);
    }

    private UserEntity user(Role role, String... jobFunctions) {
        UserEntity user = new UserEntity();
        user.setEmail("spa-time-" + UUID.randomUUID() + "@example.com");
        user.setName(user.getEmail());
        user.setPasswordHash("unused");
        user.setRole(role);
        user.setActive(true);
        user.setJobFunctions(jobFunctions);
        return userRepository.saveAndFlush(user);
    }

    private Booking stay(LocalDate checkIn, LocalDate checkOut, boolean checkedIn) {
        Booking booking = bookingService.createStaffBooking(
                new StaffBookingCreateInput(room.getId(), "Spa Time Guest", checkIn.toString(), checkOut.toString(), BookingChannel.WALK_IN, 1));
        bookingService.assignRoomUnit(booking.getId(), new RoomUnitAssignmentInput().roomUnitId(unit.getId()));
        if (checkedIn) {
            occupancyService.checkIn(booking.getId());
        }
        return bookingService.getById(booking.getId());
    }

    private SpaAppointment book(Booking booking, LocalDate date, String startTime) {
        return spaAppointmentService
                .create(new SpaAppointmentCreateInput(booking.getId(), spaTable.getId(), therapist.getId(), massage.getId(), date.toString(), startTime), receptionist.getId())
                .getAppointment();
    }

    private SpaAppointmentStatus statusOf(SpaAppointment appointment) {
        return spaAppointmentRepository.findById(appointment.getId()).orElseThrow().getStatus();
    }

    // --- Date and time of a booking --------------------------------------------------------------

    @Test
    void aTreatmentCannotBeBookedInThePast_butTheSlotUnderWayCan() {
        Booking booking = stay(TODAY, TODAY.plusDays(3), true);

        // 12:20 now: the 12:00 slot is the current one.
        assertThat(book(booking, TODAY, "12:00").getStatus()).isEqualTo(SpaAppointmentStatus.BOOKED);
        assertThat(book(booking, TODAY, "13:30").getStatus()).isEqualTo(SpaAppointmentStatus.BOOKED);
        // The slot before it is over.
        assertThatThrownBy(() -> book(booking, TODAY, "11:30"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already passed");
    }

    @Test
    void aTreatmentCannotBeBookedOnAnEarlierDay_evenInsideTheStay() {
        Booking booking = stay(TODAY.minusDays(2), TODAY.plusDays(2), true);

        assertThatThrownBy(() -> book(booking, TODAY.minusDays(1), "10:00"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already passed");
    }

    @Test
    void movingAnAppointmentIntoThePastIsRefusedToo() {
        Booking booking = stay(TODAY, TODAY.plusDays(3), true);
        SpaAppointment appointment = book(booking, TODAY.plusDays(1), "10:00");

        assertThatThrownBy(() -> spaAppointmentService.updateSchedule(
                        appointment.getId(), new SpaAppointmentScheduleInput(spaTable.getId(), therapist.getId(), TODAY.toString(), "11:00"), receptionist.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already passed");
        assertThat(spaAppointmentService.updateSchedule(
                        appointment.getId(), new SpaAppointmentScheduleInput(spaTable.getId(), therapist.getId(), TODAY.toString(), "12:00"), receptionist.getId())
                .getStartTime()).isEqualTo("12:00");
    }

    @Test
    void aGuestInTheHouseIsHeldToTheStayLikeEveryoneElse() {
        Booking booking = stay(TODAY, TODAY.plusDays(2), true);

        assertThatThrownBy(() -> book(booking, TODAY.plusDays(3), "10:00"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("doesn't include");
        assertThat(book(booking, TODAY.plusDays(2), "10:00").getStatus()).isEqualTo(SpaAppointmentStatus.BOOKED);
    }

    // --- A shorter stay takes the spa with it ---------------------------------------------------

    @Test
    void shorteningTheStay_cancelsTheStillAheadAppointmentsOutsideIt_andKeepsTheOthers() {
        Booking booking = stay(TODAY, TODAY.plusDays(4), false);
        SpaAppointment inside = book(booking, TODAY.plusDays(1), "10:00");
        SpaAppointment onNewDepartureDay = book(booking, TODAY.plusDays(2), "10:00");
        SpaAppointment outside = book(booking, TODAY.plusDays(3), "10:00");
        SpaAppointment onOldDepartureDay = book(booking, TODAY.plusDays(4), "10:00");

        bookingService.updateSchedule(booking.getId(), new BookingScheduleInput(TODAY.toString(), TODAY.plusDays(2).toString()).roomUnitId(unit.getId()), false);

        assertThat(statusOf(inside)).isEqualTo(SpaAppointmentStatus.BOOKED);
        // The departure day itself is still inside the stay.
        assertThat(statusOf(onNewDepartureDay)).isEqualTo(SpaAppointmentStatus.BOOKED);
        assertThat(statusOf(outside)).isEqualTo(SpaAppointmentStatus.CANCELLED);
        assertThat(statusOf(onOldDepartureDay)).isEqualTo(SpaAppointmentStatus.CANCELLED);
    }

    @Test
    void anEarlyCheckOut_cancelsTheAppointmentsAfterTheNewDeparture() {
        Booking booking = stay(TODAY, TODAY.plusDays(3), true);
        SpaAppointment later = book(booking, TODAY.plusDays(2), "10:00");
        SpaAppointment stillToday = book(booking, TODAY, "15:00");

        occupancyService.checkOut(booking.getId(), new CheckOutInput().shortenStay(true).chargeUnusedNights(false));

        assertThat(statusOf(later)).isEqualTo(SpaAppointmentStatus.CANCELLED);
        // Today is the new last day, so a treatment later today is not "outside" the stay.
        assertThat(statusOf(stillToday)).isEqualTo(SpaAppointmentStatus.BOOKED);
    }

    @Test
    void anAppointmentThatIsAlreadyPaid_isLeftWhenTheStayIsShortened() {
        Booking booking = stay(TODAY, TODAY.plusDays(4), false);
        SpaAppointment paid = book(booking, TODAY.plusDays(3), "10:00");
        Order order = orderService.create(
                new OrderCreateInput().bookingId(booking.getId()).spaAppointmentId(paid.getId()).items(List.of(new OrderItemInput(massage.getId(), 1))), receptionist.getId());
        OrderEntity paidOrder = orderRepository.findById(order.getId()).orElseThrow();
        paidOrder.setStatus(OrderStatus.PAID);
        orderRepository.saveAndFlush(paidOrder);
        SpaAppointment unpaid = book(booking, TODAY.plusDays(3), "13:00");

        bookingService.updateSchedule(booking.getId(), new BookingScheduleInput(TODAY.toString(), TODAY.plusDays(2).toString()).roomUnitId(unit.getId()), false);

        assertThat(statusOf(paid)).isEqualTo(SpaAppointmentStatus.BOOKED);
        assertThat(statusOf(unpaid)).isEqualTo(SpaAppointmentStatus.CANCELLED);
    }

    @Test
    void anAppointmentOfAnotherBooking_isNeverTouched() {
        Booking mine = stay(TODAY, TODAY.plusDays(4), false);
        RoomUnitEntity otherUnit = new RoomUnitEntity();
        otherUnit.setRoomId(room.getId());
        otherUnit.setLabel("SpaTime2-" + UUID.randomUUID().toString().substring(0, 8));
        otherUnit.setActive(true);
        roomUnitRepository.saveAndFlush(otherUnit);
        Booking other = bookingService.createStaffBooking(
                new StaffBookingCreateInput(room.getId(), "Other Guest", TODAY.toString(), TODAY.plusDays(4).toString(), BookingChannel.WALK_IN, 1));
        bookingService.assignRoomUnit(other.getId(), new RoomUnitAssignmentInput().roomUnitId(otherUnit.getId()));
        SpaAppointment othersAppointment = book(other, TODAY.plusDays(3), "10:00");

        bookingService.updateSchedule(mine.getId(), new BookingScheduleInput(TODAY.toString(), TODAY.plusDays(2).toString()).roomUnitId(unit.getId()), false);

        SpaAppointmentEntity reloaded = spaAppointmentRepository.findById(othersAppointment.getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(SpaAppointmentStatus.BOOKED);
    }
}
