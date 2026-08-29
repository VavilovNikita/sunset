package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.BookingMapper;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingFolio;
import com.sunsetbeach.model.BookingPosOrder;
import com.sunsetbeach.model.BookingPosOrderItem;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.BookingScheduleQuote;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import jakarta.persistence.criteria.Predicate;
import org.openapitools.jackson.nullable.JsonNullable;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final String SERIALIZATION_FAILURE_SQLSTATE = "40001";

    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final BookingRepository bookingRepository;
    private final BookingWriter bookingWriter;
    private final BookingMapper bookingMapper;
    private final EmailService emailService;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;

    public BookingService(
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            BookingRepository bookingRepository,
            BookingWriter bookingWriter,
            BookingMapper bookingMapper,
            EmailService emailService,
            PaymentRepository paymentRepository,
            OrderItemRepository orderItemRepository,
            MenuItemRepository menuItemRepository) {
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.bookingRepository = bookingRepository;
        this.bookingWriter = bookingWriter;
        this.bookingMapper = bookingMapper;
        this.emailService = emailService;
        this.paymentRepository = paymentRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
    }

    public Booking createBooking(BookingCreateInput input) {
        LocalDate checkIn = LocalDate.parse(input.getCheckIn());
        LocalDate checkOut = LocalDate.parse(input.getCheckOut());
        if (!checkIn.isBefore(checkOut)) {
            throw ValidationException.field("checkOut", "checkIn must be before checkOut");
        }

        RoomEntity room = roomRepository.findById(input.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));

        BookingEntity saved;
        try {
            saved = bookingWriter.insert(
                    room, input.getGuestName(), input.getGuestEmail(), input.getGuestPhone(), checkIn, checkOut);
        } catch (DataAccessException | TransactionSystemException e) {
            if (isSerializationFailure(e)) {
                throw new ConflictException("Someone just booked these dates — please try again.");
            }
            throw e;
        }

        emailService.sendNewBookingEmail(saved, room);
        return bookingMapper.toDto(saved, room, null);
    }

    /**
     * Front-desk counterpart of {@link #createBooking} for {@code POST /bookings/staff}: same
     * server-computed pricing and race-safety, but no new-booking notification email (that email
     * is for a guest-submitted inquiry, not every reservation staff makes at the counter), and,
     * if {@code roomUnitId} is given, the physical room is assigned atomically alongside the
     * booking itself - see {@link BookingWriter#insertStaff}.
     */
    public Booking createStaffBooking(StaffBookingCreateInput input) {
        LocalDate checkIn = LocalDate.parse(input.getCheckIn());
        LocalDate checkOut = LocalDate.parse(input.getCheckOut());
        if (!checkIn.isBefore(checkOut)) {
            throw ValidationException.field("checkOut", "checkIn must be before checkOut");
        }

        RoomEntity room = roomRepository.findById(input.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        String roomUnitId = input.getRoomUnitId().isPresent() ? input.getRoomUnitId().get() : null;

        BookingEntity saved;
        try {
            saved = bookingWriter.insertStaff(
                    room,
                    input.getGuestName(),
                    input.getGuestEmail().isPresent() ? input.getGuestEmail().get() : null,
                    input.getGuestPhone().isPresent() ? input.getGuestPhone().get() : null,
                    checkIn,
                    checkOut,
                    roomUnitId);
        } catch (DataAccessException | TransactionSystemException e) {
            if (isSerializationFailure(e)) {
                throw new ConflictException("Someone just booked these dates — please try again.");
            }
            throw e;
        }

        return bookingMapper.toDto(saved, room, findRoomUnit(saved.getRoomUnitId()));
    }

    @Transactional
    public Booking updateStatus(String id, BookingStatusInput input) {
        BookingEntity booking = bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        booking.setStatus(input.getStatus());
        // paymentNote is optional+nullable: an omitted field leaves the existing value alone
        // (matches Prisma skipping `undefined` update data), an explicit null clears it.
        if (input.getPaymentNote().isPresent()) {
            String note = input.getPaymentNote().get();
            String trimmed = note != null ? note.trim() : null;
            if (PaymentNoteValidator.looksLikeCardNumber(trimmed)) {
                throw ValidationException.field(
                        "paymentNote",
                        "This looks like it contains a payment card number. Don't store full card numbers here - "
                                + "use a reference number or the last 4 digits instead.");
            }
            booking.setPaymentNote(trimmed);
        }
        // flush so @UpdateTimestamp (regenerated on every save) is on the object before mapping
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        RoomEntity room = roomRepository.findById(saved.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        emailService.sendGuestStatusEmail(saved, room);
        return bookingMapper.toDto(saved, room, findRoomUnit(saved.getRoomUnitId()));
    }

    @Transactional(readOnly = true)
    public List<Booking> list(String from, String to, BookingStatus status) {
        List<BookingEntity> bookings = bookingRepository.findAll(buildSpecification(from, to, status));
        return bookings.stream().map(b -> bookingMapper.toDto(b, b.getRoom(), b.getRoomUnit())).toList();
    }

    @Transactional(readOnly = true)
    public Booking getById(String id) {
        BookingEntity booking = bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        RoomEntity room = roomRepository.findById(booking.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        return bookingMapper.toDto(booking, room, findRoomUnit(booking.getRoomUnitId()));
    }

    /**
     * Assigns ({@code roomUnitId} non-null) or clears ({@code roomUnitId} null) the physical
     * room for a booking. Mirrors {@link #createBooking}'s race-safety story: the actual
     * validation and write happen inside {@link BookingWriter#assignRoomUnit}, which runs
     * SERIALIZABLE so two concurrent assignments of the same unit can't both succeed; a
     * serialization failure here is translated to the same "try again" conflict as booking
     * creation.
     */
    public Booking assignRoomUnit(String bookingId, RoomUnitAssignmentInput input) {
        String roomUnitId = requireRoomUnitIdPresent(input.getRoomUnitId());

        BookingEntity saved;
        try {
            saved = roomUnitId == null ? bookingWriter.unassignRoomUnit(bookingId) : bookingWriter.assignRoomUnit(bookingId, roomUnitId);
        } catch (DataAccessException | TransactionSystemException e) {
            if (isSerializationFailure(e)) {
                throw new ConflictException("Someone just assigned this room — please try again.");
            }
            throw e;
        }

        RoomEntity room = roomRepository.findById(saved.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        return bookingMapper.toDto(saved, room, findRoomUnit(saved.getRoomUnitId()));
    }

    /**
     * Changes a booking's dates and/or physical room in one operation - the write path behind
     * {@code PATCH /bookings/{id}/schedule}. Mirrors {@link #createBooking}'s race-safety story:
     * the actual validation/write happens in {@link BookingWriter#updateSchedule}, which runs
     * SERIALIZABLE and excludes this booking's own current reservation from every conflict
     * check; a serialization failure here is translated to the same "try again" conflict as
     * booking creation.
     */
    public Booking updateSchedule(String bookingId, BookingScheduleInput input) {
        LocalDate checkIn = LocalDate.parse(input.getCheckIn());
        LocalDate checkOut = LocalDate.parse(input.getCheckOut());
        if (!checkIn.isBefore(checkOut)) {
            throw ValidationException.field("checkOut", "checkIn must be before checkOut");
        }
        String roomUnitId = requireRoomUnitIdPresent(input.getRoomUnitId());

        BookingEntity saved;
        try {
            saved = bookingWriter.updateSchedule(bookingId, checkIn, checkOut, roomUnitId);
        } catch (DataAccessException | TransactionSystemException e) {
            if (isSerializationFailure(e)) {
                throw new ConflictException("Someone just changed this booking's schedule — please try again.");
            }
            throw e;
        }

        RoomEntity room = roomRepository.findById(saved.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        return bookingMapper.toDto(saved, room, findRoomUnit(saved.getRoomUnitId()));
    }

    /**
     * Non-mutating preview for {@code POST /bookings/{id}/schedule/quote} - see
     * {@link BookingWriter#quoteSchedule}. No serialization-failure handling needed here: this
     * read never writes, so there is nothing for Postgres to report a conflict on.
     */
    @Transactional(readOnly = true)
    public BookingScheduleQuote quoteSchedule(String bookingId, BookingScheduleInput input) {
        LocalDate checkIn = LocalDate.parse(input.getCheckIn());
        LocalDate checkOut = LocalDate.parse(input.getCheckOut());
        if (!checkIn.isBefore(checkOut)) {
            throw ValidationException.field("checkOut", "checkIn must be before checkOut");
        }
        String roomUnitId = requireRoomUnitIdPresent(input.getRoomUnitId());

        BookingWriter.ScheduleQuote quote = bookingWriter.quoteSchedule(bookingId, checkIn, checkOut, roomUnitId);
        return new BookingScheduleQuote(PriceFormat.asDecimalString(quote.totalPrice()), quote.nights(), quote.available(), quote.reason());
    }

    /**
     * {@code roomUnitId} must be present in the request body (a string or explicit {@code null})
     * for {@code RoomUnitAssignmentInput}/{@code BookingScheduleInput} - deliberately checked
     * here rather than via a generated {@code @NotNull}, because
     * {@code org.openapitools:jackson-databind-nullable}'s {@code @UnwrapByDefault} Jakarta
     * {@code ValueExtractor} for {@code JsonNullable} would make {@code @NotNull} validate the
     * *unwrapped* value instead, incorrectly rejecting the exact {@code null} this field exists
     * to accept (see both schemas' descriptions in openapi.yaml).
     */
    private static String requireRoomUnitIdPresent(JsonNullable<String> roomUnitId) {
        if (!roomUnitId.isPresent()) {
            throw ValidationException.field("roomUnitId", "roomUnitId is required (send null to unassign)");
        }
        return roomUnitId.get();
    }

    private RoomUnitEntity findRoomUnit(String roomUnitId) {
        return roomUnitId != null ? roomUnitRepository.findById(roomUnitId).orElse(null) : null;
    }

    /**
     * folio(booking) = booking.totalPrice + sum of ROOM_CHARGE Payment.amount for this
     * booking. Computed on the fly, never stored - this is the one place that calculation is
     * allowed to happen; callers (e.g. {@link #getFolio}, {@link #listPosOrders}) should go
     * through this method (or {@link #computeFolioBreakdown}/{@link #roomChargePayments})
     * rather than summing Payments themselves.
     */
    @Transactional(readOnly = true)
    public BigDecimal computeFolio(String bookingId) {
        return computeFolioBreakdown(bookingId).folioTotal();
    }

    /** Same numbers as {@link #computeFolio}, broken out for `GET /bookings/{id}/folio`. */
    @Transactional(readOnly = true)
    public BookingFolio getFolio(String bookingId) {
        FolioBreakdown breakdown = computeFolioBreakdown(bookingId);
        return new BookingFolio(
                PriceFormat.asDecimalString(breakdown.roomTotal()),
                PriceFormat.asDecimalString(breakdown.roomChargesTotal()),
                PriceFormat.asDecimalString(breakdown.folioTotal()),
                breakdown.roomChargeCount());
    }

    /** The one place booking.totalPrice + Σ ROOM_CHARGE Payment.amount is actually computed. */
    private FolioBreakdown computeFolioBreakdown(String bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        List<PaymentEntity> payments = roomChargePayments(bookingId);
        BigDecimal roomCharges = payments.stream().map(PaymentEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new FolioBreakdown(booking.getTotalPrice(), roomCharges, payments.size());
    }

    private record FolioBreakdown(BigDecimal roomTotal, BigDecimal roomChargesTotal, int roomChargeCount) {
        BigDecimal folioTotal() {
            return roomTotal.add(roomChargesTotal);
        }
    }

    /**
     * One entry per ROOM_CHARGE Payment against this booking - the same underlying data
     * {@link #computeFolio} sums, just itemized instead of totaled. Powers the admin "Room
     * charges" view without a per-row GET /orders/{id} round trip.
     */
    @Transactional(readOnly = true)
    public List<BookingPosOrder> listPosOrders(String bookingId) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new NotFoundException("Booking not found");
        }

        List<PaymentEntity> payments = roomChargePayments(bookingId);
        if (payments.isEmpty()) {
            return List.of();
        }

        List<String> orderIds = payments.stream().map(PaymentEntity::getOrderId).toList();
        Map<String, List<OrderItemEntity>> itemsByOrderId = orderItemRepository.findByOrderIdIn(orderIds).stream()
                .collect(Collectors.groupingBy(OrderItemEntity::getOrderId));

        List<String> menuItemIds =
                itemsByOrderId.values().stream().flatMap(List::stream).map(OrderItemEntity::getMenuItemId).distinct().toList();
        Map<String, String> menuItemNames =
                menuItemRepository.findAllById(menuItemIds).stream().collect(Collectors.toMap(MenuItemEntity::getId, MenuItemEntity::getName));

        return payments.stream()
                .sorted(Comparator.comparing(PaymentEntity::getCreatedAt))
                .map(payment -> toBookingPosOrder(
                        payment, itemsByOrderId.getOrDefault(payment.getOrderId(), List.of()), menuItemNames))
                .toList();
    }

    /** Shared by {@link #computeFolio} and {@link #listPosOrders} - the one query both read from. */
    private List<PaymentEntity> roomChargePayments(String bookingId) {
        return paymentRepository.findByBookingIdAndMethod(bookingId, PaymentMethod.ROOM_CHARGE);
    }

    private static BookingPosOrder toBookingPosOrder(
            PaymentEntity payment, List<OrderItemEntity> items, Map<String, String> menuItemNames) {
        List<BookingPosOrderItem> itemDtos = items.stream()
                .map(item -> new BookingPosOrderItem(
                        menuItemNames.getOrDefault(item.getMenuItemId(), "Unknown item"), item.getQuantity()))
                .toList();
        return new BookingPosOrder(
                payment.getOrderId(),
                PriceFormat.asDecimalString(payment.getAmount()),
                TimestampFormat.toUtc(payment.getCreatedAt()),
                itemDtos);
    }

    @Transactional(readOnly = true)
    public String exportCsv(String from, String to, BookingStatus status) {
        List<BookingEntity> bookings = bookingRepository.findAll(buildSpecification(from, to, status));
        return buildCsv(bookings);
    }

    /** Shared by {@link #list} and {@link #exportCsv} - same filters, same `checkIn` ascending order. */
    private static Specification<BookingEntity> buildSpecification(String from, String to, BookingStatus status) {
        LocalDate fromDate = from != null ? LocalDate.parse(from) : null;
        LocalDate toDate = to != null ? LocalDate.parse(to) : null;

        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                root.fetch("room");
            }
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                predicates.add(cb.greaterThan(root.get("checkOut"), fromDate));
            }
            if (toDate != null) {
                // <=, not < : checkIn/checkOut is a half-open [checkIn, checkOut) stay window,
                // but from/to is a closed [from, to] query range - a booking checking in exactly
                // on `to` (e.g. from=to=today, checkIn=today) must still match. A strict "<"
                // here would silently drop same-day check-ins from range queries.
                predicates.add(cb.lessThanOrEqualTo(root.get("checkIn"), toDate));
            }
            query.orderBy(cb.asc(root.get("checkIn")));
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private static String buildCsv(List<BookingEntity> bookings) {
        CsvBuilder csv = new CsvBuilder();
        csv.row("ID", "Room", "Guest", "Email", "Phone", "Check-in", "Check-out", "Total", "Status", "Payment note", "Created at");
        for (BookingEntity b : bookings) {
            csv.row(
                    b.getId(),
                    b.getRoom().getName(),
                    b.getGuestName(),
                    b.getGuestEmail(),
                    b.getGuestPhone(),
                    b.getCheckIn().toString(),
                    b.getCheckOut().toString(),
                    PriceFormat.asDecimalString(b.getTotalPrice()),
                    b.getStatus().getValue(),
                    b.getPaymentNote() != null ? b.getPaymentNote() : "",
                    DateRangeUtil.formatIsoInstant(b.getCreatedAt()));
        }
        return csv.toString();
    }

    private static boolean isSerializationFailure(Throwable ex) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof SQLException sqlException
                    && SERIALIZATION_FAILURE_SQLSTATE.equals(sqlException.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
