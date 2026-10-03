package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.FolioPaymentEntity;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.entity.MenuItemEntity;
import com.sunsetbeach.entity.OrderItemEntity;
import com.sunsetbeach.entity.PaymentEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.SqlStates;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.BookingMapper;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingPurpose;
import com.sunsetbeach.model.BookingCreateInput;
import com.sunsetbeach.model.BookingFolio;
import com.sunsetbeach.model.BookingGuestLinkInput;
import com.sunsetbeach.model.BookingPage;
import com.sunsetbeach.model.BookingPosOrder;
import com.sunsetbeach.model.BookingPosOrderItem;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.BookingScheduleQuote;
import com.sunsetbeach.model.BookingSortField;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.FolioPayment;
import com.sunsetbeach.model.FolioPaymentInput;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.PaymentMethod;
import com.sunsetbeach.model.RelocationInput;
import com.sunsetbeach.model.RelocationUndoInput;
import com.sunsetbeach.model.RepriceInput;
import com.sunsetbeach.model.RepriceQuote;
import com.sunsetbeach.model.RoomUnitAssignmentInput;
import com.sunsetbeach.model.SortDirection;
import com.sunsetbeach.model.StaffBookingCreateInput;
import com.sunsetbeach.model.StaffBookingQuoteInput;
import com.sunsetbeach.model.SwapSegmentRoomUnitInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.FolioPaymentRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.repository.MenuItemRepository;
import com.sunsetbeach.repository.OrderItemRepository;
import com.sunsetbeach.repository.PaymentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.security.StaffPrincipal;
import jakarta.persistence.criteria.Predicate;
import org.openapitools.jackson.nullable.JsonNullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private static final String SERIALIZATION_FAILURE_SQLSTATE = "40001";

    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final GuestRepository guestRepository;
    private final BookingRepository bookingRepository;
    private final BookingSegmentRepository segmentRepository;
    private final BookingWriter bookingWriter;
    private final BookingMapper bookingMapper;
    private final EmailService emailService;
    private final PaymentRepository paymentRepository;
    private final FolioPaymentRepository folioPaymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final MenuItemRepository menuItemRepository;
    private final AuditLogService auditLogService;
    private final GuestLinkService guestLinkService;
    private final LedgerService ledgerService;
    private final OverstayRule overstayRule;

    public BookingService(
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            GuestRepository guestRepository,
            BookingRepository bookingRepository,
            BookingSegmentRepository segmentRepository,
            BookingWriter bookingWriter,
            BookingMapper bookingMapper,
            EmailService emailService,
            PaymentRepository paymentRepository,
            FolioPaymentRepository folioPaymentRepository,
            OrderItemRepository orderItemRepository,
            MenuItemRepository menuItemRepository,
            AuditLogService auditLogService,
            GuestLinkService guestLinkService,
            LedgerService ledgerService,
            OverstayRule overstayRule) {
        this.overstayRule = overstayRule;
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.guestRepository = guestRepository;
        this.bookingRepository = bookingRepository;
        this.segmentRepository = segmentRepository;
        this.bookingWriter = bookingWriter;
        this.bookingMapper = bookingMapper;
        this.emailService = emailService;
        this.paymentRepository = paymentRepository;
        this.folioPaymentRepository = folioPaymentRepository;
        this.orderItemRepository = orderItemRepository;
        this.menuItemRepository = menuItemRepository;
        this.auditLogService = auditLogService;
        this.guestLinkService = guestLinkService;
        this.ledgerService = ledgerService;
    }

    private List<BookingSegmentEntity> loadSegments(String bookingId) {
        return segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
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
                    room,
                    input.getGuestName(),
                    input.getGuestEmail(),
                    input.getGuestPhone(),
                    checkIn,
                    checkOut,
                    input.getAdults(),
                    childrenOrZero(input.getChildren()));
        } catch (DataAccessException | TransactionSystemException e) {
            if (isSerializationFailure(e)) {
                throw new ConflictException("Someone just booked these dates — please try again.");
            }
            throw e;
        }

        GuestEntity guest = linkGuestQuietly(saved);
        emailService.sendNewBookingEmail(saved, room);
        return bookingMapper.toDto(saved, room, null, guest, loadSegments(saved.getId()));
    }

    /**
     * Find-or-create the booking's Guest card by email (see {@link GuestLinkService}) - after the
     * booking has committed, never inside {@link BookingWriter}'s SERIALIZABLE transaction, where
     * reading {@code Guest} would add a new source of serialization conflicts to every booking.
     * Same "never break the operation it accompanies" rule as printing/email/audit: a booking that
     * failed to link is still a booking, and staff can link it by hand.
     */
    private GuestEntity linkGuestQuietly(BookingEntity booking) {
        try {
            GuestEntity guest = guestLinkService.linkNewBooking(booking.getId());
            booking.setGuestId(guest != null ? guest.getId() : null);
            return guest;
        } catch (RuntimeException e) {
            log.error("Failed to link booking {} to a guest card", booking.getId(), e);
            return null;
        }
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
        // Checked before the booking exists, so a stale picked card is a 404 with nothing created,
        // not a booking that silently fell back to find-or-create by email.
        String pickedGuestId = input.getGuestId() != null && input.getGuestId().isPresent() ? input.getGuestId().get() : null;
        if (pickedGuestId != null && !guestRepository.existsById(pickedGuestId)) {
            throw new NotFoundException("Guest not found");
        }

        BookingEntity saved;
        try {
            saved = bookingWriter.insertStaff(
                    room,
                    input.getGuestName(),
                    input.getGuestEmail().isPresent() ? input.getGuestEmail().get() : null,
                    input.getGuestPhone().isPresent() ? input.getGuestPhone().get() : null,
                    checkIn,
                    checkOut,
                    input.getChannel(),
                    input.getPurpose() != null ? input.getPurpose() : BookingPurpose.STANDARD,
                    input.getAdults(),
                    childrenOrZero(input.getChildren()),
                    roomUnitId);
        } catch (DataAccessException | TransactionSystemException e) {
            if (isSerializationFailure(e)) {
                throw new ConflictException("Someone just booked these dates — please try again.");
            }
            throw e;
        }

        GuestEntity guest = pickedGuestId != null ? linkPickedGuestQuietly(saved, pickedGuestId) : linkGuestQuietly(saved);
        RoomUnitEntity assignedUnit = findRoomUnit(saved.getRoomUnitId());
        auditLogService.record(
                AuditAction.BOOKING_CREATED,
                AuditEntityType.BOOKING,
                saved.getId(),
                "Staff booking created for " + saved.getGuestName() + " in " + room.getName() + " (" + checkIn + " to " + checkOut + ")"
                        + (assignedUnit != null ? "; room " + assignedUnit.getLabel() : ""));
        return bookingMapper.toDto(saved, room, assignedUnit, guest, loadSegments(saved.getId()));
    }

    /**
     * The staff-given reason for a cancellation, trimmed, or null when this change isn't into
     * {@code CANCELLED} or none was given. It lives only in the audit entry (see
     * {@code BookingStatusInput.cancellationReason}); free text, so it is capped at the spec's
     * 500 characters by Bean Validation before it gets here.
     */
    private static String cancellationReason(BookingStatusInput input, BookingStatus newStatus) {
        if (newStatus != BookingStatus.CANCELLED || input.getCancellationReason() == null || !input.getCancellationReason().isPresent()) {
            return null;
        }
        String reason = input.getCancellationReason().get();
        return reason == null || reason.isBlank() ? null : reason.trim();
    }

    /** {@link #linkGuestQuietly}'s counterpart for a card staff picked in the create form - same after-commit, never-breaks-the-booking contract. */
    private GuestEntity linkPickedGuestQuietly(BookingEntity booking, String guestId) {
        try {
            GuestEntity guest = guestLinkService.linkNewBookingToGuest(booking.getId(), guestId);
            booking.setGuestId(guest.getId());
            return guest;
        } catch (RuntimeException e) {
            log.error("Failed to link booking {} to picked guest {}", booking.getId(), guestId, e);
            return null;
        }
    }

    /**
     * Prices a not-yet-created staff booking for {@code POST /bookings/staff/quote} - see
     * {@link BookingWriter#quoteStaff}. Same date and room checks as {@link #createStaffBooking},
     * so a request the create call would reject as a 400/404 is rejected the same way here.
     */
    public BookingScheduleQuote quoteStaffBooking(StaffBookingQuoteInput input) {
        LocalDate checkIn = LocalDate.parse(input.getCheckIn());
        LocalDate checkOut = LocalDate.parse(input.getCheckOut());
        if (!checkIn.isBefore(checkOut)) {
            throw ValidationException.field("checkOut", "checkIn must be before checkOut");
        }
        RoomEntity room = roomRepository.findById(input.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        String roomUnitId = input.getRoomUnitId() != null && input.getRoomUnitId().isPresent() ? input.getRoomUnitId().get() : null;
        BookingWriter.ScheduleQuote quote = bookingWriter.quoteStaff(room, checkIn, checkOut, roomUnitId);
        return new BookingScheduleQuote(PriceFormat.asDecimalString(quote.totalPrice()), quote.nights(), quote.available(), quote.reason());
    }

    /**
     * {@code children} is optional with a default of 0 in openapi.yaml, but an explicit JSON
     * {@code null} still arrives as null - it means the same thing as leaving it out.
     */
    private static int childrenOrZero(Integer children) {
        return children != null ? children : 0;
    }

    @Transactional
    public Booking updateStatus(String id, BookingStatusInput input) {
        // Row-locked: two concurrent "mark PAID" requests must not both see the old status and
        // both post a room settlement to the ledger below.
        BookingEntity booking = bookingRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        BookingStatus oldStatus = booking.getStatus();
        String oldPaymentNote = booking.getPaymentNote();
        booking.setStatus(input.getStatus());
        // paymentNote is optional+nullable: an omitted field leaves the existing value alone
        // (matches Prisma skipping `undefined` update data), an explicit null clears it.
        boolean paymentNoteChanged = false;
        if (input.getPaymentNote().isPresent()) {
            String note = input.getPaymentNote().get();
            String trimmed = note != null ? note.trim() : null;
            if (PaymentNoteValidator.looksLikeCardNumber(trimmed)) {
                throw ValidationException.field(
                        "paymentNote",
                        "This looks like it contains a payment card number. Don't store full card numbers here - "
                                + "use a reference number or the last 4 digits instead.");
            }
            paymentNoteChanged = !Objects.equals(oldPaymentNote, trimmed);
            booking.setPaymentNote(trimmed);
        }
        // channel is optional but not nullable: omitted (null here) leaves it alone, and there is
        // no "clear" case - a booking always has a channel.
        BookingChannel oldChannel = booking.getChannel();
        if (input.getChannel() != null) {
            booking.setChannel(input.getChannel());
        }
        // purpose/adults/children: same shape as channel - omitted leaves it alone, no clear case.
        BookingPurpose oldPurpose = booking.getPurpose();
        if (input.getPurpose() != null) {
            booking.setPurpose(input.getPurpose());
        }
        int oldAdults = booking.getAdults();
        int oldChildren = booking.getChildren();
        if (input.getAdults() != null) {
            booking.setAdults(input.getAdults());
        }
        if (input.getChildren() != null) {
            booking.setChildren(input.getChildren());
        }
        // flush so @UpdateTimestamp (regenerated on every save) is on the object before mapping
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        // PAID is the only record of the room portion being collected - see LedgerService's
        // class javadoc. Same transaction: a status change the ledger missed is worse than none.
        if (oldStatus != BookingStatus.PAID && saved.getStatus() == BookingStatus.PAID) {
            ledgerService.postBookingSettlement(saved);
        } else if (oldStatus == BookingStatus.PAID && saved.getStatus() != BookingStatus.PAID) {
            ledgerService.reverseBookingSettlement(saved);
        }

        RoomEntity room = roomRepository.findById(saved.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        boolean statusChanged = oldStatus != saved.getStatus();
        // Only on a real status change - otherwise correcting the channel or payment note on a
        // PAID booking would re-send the guest a "your payment has been received" email.
        if (statusChanged) {
            emailService.sendGuestStatusEmail(saved, room);
        }

        // A channel/purpose/party-size correction rides on BOOKING_STATUS_CHANGED (this endpoint's
        // own action) rather than a new AuditAction, but is always named in the summary - never an
        // unremarked update.
        boolean channelChanged = oldChannel != saved.getChannel();
        boolean purposeChanged = oldPurpose != saved.getPurpose();
        boolean guestsChanged = oldAdults != saved.getAdults() || oldChildren != saved.getChildren();
        if (statusChanged || channelChanged || purposeChanged || guestsChanged) {
            List<String> changes = new ArrayList<>();
            if (statusChanged) {
                changes.add("status changed from " + oldStatus.getValue() + " to " + saved.getStatus().getValue());
            }
            if (channelChanged) {
                changes.add("channel changed from " + (oldChannel != null ? oldChannel.getValue() : "none") + " to " + saved.getChannel().getValue());
            }
            if (purposeChanged) {
                changes.add("purpose changed from " + oldPurpose.getValue() + " to " + saved.getPurpose().getValue());
            }
            if (guestsChanged) {
                changes.add("guests changed from " + partySize(oldAdults, oldChildren) + " to " + partySize(saved.getAdults(), saved.getChildren()));
            }
            String summary = String.join("; ", changes);
            String reason = statusChanged ? cancellationReason(input, saved.getStatus()) : null;
            auditLogService.record(
                    AuditAction.BOOKING_STATUS_CHANGED,
                    AuditEntityType.BOOKING,
                    saved.getId(),
                    Character.toUpperCase(summary.charAt(0)) + summary.substring(1) + " for " + saved.getGuestName() + " in " + room.getName()
                            + (reason != null ? ". Reason: " + reason : ""));
        }
        // Content is deliberately never included in the summary - paymentNote is free text staff
        // may (against guidance) use for something sensitive; recording that it changed is
        // enough for the audit trail without duplicating that risk into a second table.
        if (paymentNoteChanged) {
            auditLogService.record(
                    AuditAction.BOOKING_PAYMENT_NOTE_CHANGED,
                    AuditEntityType.BOOKING,
                    saved.getId(),
                    "Payment note " + (saved.getPaymentNote() != null ? "updated" : "cleared") + " for " + saved.getGuestName()
                            + " in " + room.getName());
        }

        return bookingMapper.toDto(saved, room, findRoomUnit(saved.getRoomUnitId()), findGuest(saved.getGuestId()), loadSegments(saved.getId()));
    }

    private static String partySize(int adults, int children) {
        return adults + (adults == 1 ? " adult" : " adults") + ", " + children + (children == 1 ? " child" : " children");
    }

    @Transactional(readOnly = true)
    public List<Booking> list(String from, String to, BookingStatus status, String guestName) {
        List<BookingEntity> bookings = bookingRepository.findAll(buildSpecification(from, to, status, guestName, overstayRule.today()));
        Map<String, List<BookingSegmentEntity>> segmentsByBookingId = bookings.isEmpty()
                ? Map.of()
                : segmentRepository.findByBookingIdIn(bookings.stream().map(BookingEntity::getId).toList()).stream()
                        .collect(Collectors.groupingBy(BookingSegmentEntity::getBookingId));
        return bookings.stream()
                .map(b -> bookingMapper.toDto(
                        b, b.getRoom(), b.getRoomUnit(), b.getGuest(), segmentsByBookingId.getOrDefault(b.getId(), List.of())))
                .toList();
    }

    /**
     * {@code GET /bookings/search} - the admin Bookings list. The same filters as {@link #list}
     * (including the {@link OverstayRule} widening), plus free text over the guest's own contact
     * fields, the SiteMinder reference and the booking id, sorted and paged in the database so a
     * long history never travels to the screen whole. A tie on the sort column falls back to
     * {@code id}, or a booking could appear on two pages (or none) while paging.
     */
    @Transactional(readOnly = true)
    public BookingPage search(
            String q, String from, String to, BookingStatus status, BookingSortField sort, SortDirection direction, Integer page, Integer pageSize) {
        int pageNumber = page != null ? page : 0;
        int size = pageSize != null ? pageSize : 50;
        BookingSortField sortField = sort != null ? sort : BookingSortField.CHECK_IN;
        Sort.Direction dir = direction == SortDirection.ASC ? Sort.Direction.ASC : Sort.Direction.DESC;
        Sort.Order primary = new Sort.Order(dir, sortProperty(sortField));
        if (sortField == BookingSortField.GUEST_NAME || sortField == BookingSortField.ROOM) {
            primary = primary.ignoreCase();
        }
        Sort order = Sort.by(primary, Sort.Order.asc("id"));

        Specification<BookingEntity> spec = buildSpecification(from, to, status, null, overstayRule.today()).and(textSearch(q));
        Page<BookingEntity> result = bookingRepository.findAll(spec, PageRequest.of(pageNumber, size, order));
        List<BookingEntity> bookings = result.getContent();
        Map<String, List<BookingSegmentEntity>> segmentsByBookingId = bookings.isEmpty()
                ? Map.of()
                : segmentRepository.findByBookingIdIn(bookings.stream().map(BookingEntity::getId).toList()).stream()
                        .collect(Collectors.groupingBy(BookingSegmentEntity::getBookingId));
        List<Booking> items = bookings.stream()
                .map(b -> bookingMapper.toDto(
                        b, b.getRoom(), b.getRoomUnit(), b.getGuest(), segmentsByBookingId.getOrDefault(b.getId(), List.of())))
                .toList();
        return new BookingPage(items, pageNumber, size, (int) result.getTotalElements());
    }

    private static String sortProperty(BookingSortField field) {
        return switch (field) {
            case GUEST_NAME -> "guestName";
            case ROOM -> "room.name";
            case CHECK_IN -> "checkIn";
            case CHECK_OUT -> "checkOut";
            case TOTAL_PRICE -> "totalPrice";
            case STATUS -> "status";
            case CREATED_AT -> "createdAt";
        };
    }

    private static Specification<BookingEntity> textSearch(String q) {
        if (q == null || q.isBlank()) {
            return (root, query, cb) -> null;
        }
        String needle = q.trim().toLowerCase(Locale.ROOT);
        String contains = "%" + needle + "%";
        return (root, query, cb) -> cb.or(
                cb.like(cb.lower(root.get("guestName")), contains),
                cb.like(cb.lower(root.get("guestEmail")), contains),
                cb.like(cb.lower(root.get("guestPhone")), contains),
                cb.like(cb.lower(root.get("externalReference")), contains),
                cb.like(root.get("id"), needle + "%"));
    }

    @Transactional(readOnly = true)
    public Booking getById(String id) {
        BookingEntity booking = bookingRepository.findById(id).orElseThrow(() -> new NotFoundException("Booking not found"));
        RoomEntity room = roomRepository.findById(booking.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        return bookingMapper.toDto(booking, room, findRoomUnit(booking.getRoomUnitId()), findGuest(booking.getGuestId()), loadSegments(id));
    }

    /** A guest's full stay history for {@code GET /guests/{id}} - see {@link GuestService#getDetail}. Same bulk-load shape as {@link #list}. */
    @Transactional(readOnly = true)
    public List<Booking> listByGuestId(String guestId) {
        List<BookingEntity> bookings = bookingRepository.findByGuestIdOrderByCreatedAtDesc(guestId);
        Map<String, List<BookingSegmentEntity>> segmentsByBookingId = bookings.isEmpty()
                ? Map.of()
                : segmentRepository.findByBookingIdIn(bookings.stream().map(BookingEntity::getId).toList()).stream()
                        .collect(Collectors.groupingBy(BookingSegmentEntity::getBookingId));
        return bookings.stream()
                .map(b -> bookingMapper.toDto(
                        b, b.getRoom(), b.getRoomUnit(), b.getGuest(), segmentsByBookingId.getOrDefault(b.getId(), List.of())))
                .toList();
    }

    /**
     * Links ({@code guestId} non-null) or clears ({@code guestId} null) the {@link GuestEntity}
     * for a booking - see {@code PUT /bookings/{id}/guest} in openapi.yaml for why this needs no
     * SERIALIZABLE transaction and no state check on relink, unlike {@link #assignRoomUnit}: a
     * guest link contends for nothing, so an ordinary transaction is enough. Never touches
     * {@code guestName}/{@code guestEmail}/{@code guestPhone} - those stay frozen regardless.
     */
    @Transactional
    public Booking assignGuest(String bookingId, BookingGuestLinkInput input) {
        if (!input.getGuestId().isPresent()) {
            throw ValidationException.field("guestId", "guestId is required (send null to unlink)");
        }
        String guestId = input.getGuestId().get();
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        GuestEntity guest = guestId != null ? guestRepository.findById(guestId).orElseThrow(() -> new NotFoundException("Guest not found")) : null;

        booking.setGuestId(guestId);
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        RoomEntity room = roomRepository.findById(saved.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        auditLogService.record(
                AuditAction.BOOKING_GUEST_LINKED,
                AuditEntityType.BOOKING,
                saved.getId(),
                guest != null
                        ? "Linked guest " + guest.getName() + " to booking for " + saved.getGuestName() + " in " + room.getName()
                        : "Unlinked guest from booking for " + saved.getGuestName() + " in " + room.getName());
        return bookingMapper.toDto(saved, room, findRoomUnit(saved.getRoomUnitId()), guest, loadSegments(saved.getId()));
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
        String oldRoomUnitId =
                bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found")).getRoomUnitId();

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
        RoomUnitEntity oldUnit = findRoomUnit(oldRoomUnitId);
        RoomUnitEntity newUnit = findRoomUnit(saved.getRoomUnitId());
        auditLogService.record(
                AuditAction.BOOKING_ROOM_ASSIGNED,
                AuditEntityType.BOOKING,
                saved.getId(),
                "Room " + (oldUnit != null ? oldUnit.getLabel() : "unassigned") + " → "
                        + (newUnit != null ? newUnit.getLabel() : "unassigned") + " for " + saved.getGuestName() + " (" + room.getName()
                        + ")");
        return bookingMapper.toDto(saved, room, newUnit, findGuest(saved.getGuestId()), loadSegments(saved.getId()));
    }

    /**
     * Assigns ({@code roomUnitId} non-null) or clears ({@code roomUnitId} null) the physical room
     * for one named segment - the segment-scoped sibling of {@link #assignRoomUnit}, for a
     * booking that has been split by a relocation (see {@link BookingWriter}'s class javadoc for
     * why both shapes coexist). Same race-safety story: the actual validation and write happen in
     * {@link BookingWriter#reassignSegmentRoomUnit}/{@link BookingWriter#unassignSegmentRoomUnit}.
     */
    public Booking assignSegmentRoomUnit(String bookingId, String segmentId, RoomUnitAssignmentInput input) {
        String roomUnitId = requireRoomUnitIdPresent(input.getRoomUnitId());
        BookingSegmentEntity before = segmentRepository.findById(segmentId).orElseThrow(() -> new NotFoundException("Segment not found"));
        String oldRoomUnitId = before.getRoomUnitId();

        BookingWriter.SegmentRoomUnitResult result;
        try {
            result = roomUnitId == null
                    ? bookingWriter.unassignSegmentRoomUnit(bookingId, segmentId)
                    : bookingWriter.reassignSegmentRoomUnit(bookingId, segmentId, roomUnitId);
        } catch (DataAccessException | TransactionSystemException e) {
            if (isSerializationFailure(e)) {
                throw new ConflictException("Someone just assigned this room — please try again.");
            }
            throw e;
        }

        BookingEntity saved = result.booking();
        RoomEntity segmentRoom = roomRepository.findById(result.segment().getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        RoomEntity bookingRoom = roomRepository.findById(saved.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        RoomUnitEntity oldUnit = findRoomUnit(oldRoomUnitId);
        RoomUnitEntity newUnit = result.unit();
        auditLogService.record(
                AuditAction.BOOKING_SEGMENT_ROOM_CHANGED,
                AuditEntityType.BOOKING,
                saved.getId(),
                "Room " + (oldUnit != null ? oldUnit.getLabel() : "unassigned") + " → "
                        + (newUnit != null ? newUnit.getLabel() : "unassigned") + " for one segment of " + saved.getGuestName() + "'s stay ("
                        + segmentRoom.getName() + ", " + result.segment().getCheckIn() + " to " + result.segment().getCheckOut() + ")");
        return bookingMapper.toDto(saved, bookingRoom, findRoomUnit(saved.getRoomUnitId()), findGuest(saved.getGuestId()), loadSegments(saved.getId()));
    }

    /**
     * Swaps this segment's physical room with another segment's, atomically - the write path
     * behind {@code POST /bookings/{id}/segments/{segmentId}/swap-room-unit}. The actual
     * validation and write happen in {@link BookingWriter#swapSegmentRoomUnits}, which runs
     * SERIALIZABLE against a world where neither move has happened yet (see that method's
     * javadoc). Writes one audit entry under <em>each</em> booking's own id, naming the other
     * guest, so each booking's own history shows the move - unlike every other write in this
     * class, this one genuinely changes two bookings, not one. Returns only the dragged booking's
     * own updated DTO, matching this API's single-booking-nesting convention; the other booking's
     * own change must be fetched separately (see the endpoint's own description).
     */
    public Booking swapSegmentRoomUnit(String bookingId, String segmentId, SwapSegmentRoomUnitInput input) {
        BookingWriter.SwapResult result;
        try {
            result = bookingWriter.swapSegmentRoomUnits(bookingId, segmentId, input.getWithSegmentId());
        } catch (DataAccessException | TransactionSystemException e) {
            if (isSerializationFailure(e)) {
                throw new ConflictException("Someone just changed one of these rooms — please try again.");
            }
            throw e;
        }

        BookingEntity saved = result.booking();
        BookingEntity otherSaved = result.otherBooking();
        RoomUnitEntity newUnit = findRoomUnit(result.segment().getRoomUnitId());
        RoomUnitEntity otherNewUnit = findRoomUnit(result.otherSegment().getRoomUnitId());
        auditLogService.record(
                AuditAction.BOOKING_ROOMS_SWAPPED,
                AuditEntityType.BOOKING,
                saved.getId(),
                "Swapped rooms with " + otherSaved.getGuestName() + " in " + result.room().getName() + ": now "
                        + (newUnit != null ? newUnit.getLabel() : "unassigned"));
        auditLogService.record(
                AuditAction.BOOKING_ROOMS_SWAPPED,
                AuditEntityType.BOOKING,
                otherSaved.getId(),
                "Swapped rooms with " + saved.getGuestName() + " in " + result.room().getName() + ": now "
                        + (otherNewUnit != null ? otherNewUnit.getLabel() : "unassigned"));

        return bookingMapper.toDto(saved, result.room(), newUnit, findGuest(saved.getGuestId()), loadSegments(saved.getId()));
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

        BookingEntity before = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        LocalDate oldCheckIn = before.getCheckIn();
        LocalDate oldCheckOut = before.getCheckOut();
        String oldRoomUnitId = before.getRoomUnitId();

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
        RoomUnitEntity newUnit = findRoomUnit(saved.getRoomUnitId());

        StringBuilder summary = new StringBuilder("Schedule changed for ")
                .append(saved.getGuestName())
                .append(" in ")
                .append(room.getName())
                .append(": dates ")
                .append(oldCheckIn)
                .append(" – ")
                .append(oldCheckOut)
                .append(" → ")
                .append(checkIn)
                .append(" – ")
                .append(checkOut);
        if (!Objects.equals(oldRoomUnitId, saved.getRoomUnitId())) {
            RoomUnitEntity oldUnit = findRoomUnit(oldRoomUnitId);
            summary.append("; room ")
                    .append(oldUnit != null ? oldUnit.getLabel() : "unassigned")
                    .append(" → ")
                    .append(newUnit != null ? newUnit.getLabel() : "unassigned");
        }
        auditLogService.record(AuditAction.BOOKING_SCHEDULE_CHANGED, AuditEntityType.BOOKING, saved.getId(), summary.toString());

        return bookingMapper.toDto(saved, room, newUnit, findGuest(saved.getGuestId()), loadSegments(saved.getId()));
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
     * Moves a guest to a different room (possibly a different room *type*) partway through
     * their stay - the write path behind {@code POST /bookings/{id}/relocate}. Mirrors
     * {@link #createBooking}'s race-safety story: the actual validation/write happens in
     * {@link BookingWriter#relocate}, which runs SERIALIZABLE.
     */
    public Booking relocate(String bookingId, RelocationInput input) {
        LocalDate effectiveDate = LocalDate.parse(input.getEffectiveDate());
        String newRoomUnitId = input.getRoomUnitId().isPresent() ? input.getRoomUnitId().get() : null;

        BookingWriter.RelocationResult result;
        try {
            result = bookingWriter.relocate(bookingId, effectiveDate, input.getRoomId(), newRoomUnitId);
        } catch (DataAccessException | TransactionSystemException e) {
            if (isSerializationFailure(e)) {
                throw new ConflictException("Someone just booked these dates — please try again.");
            }
            throw e;
        }

        BookingEntity saved = result.booking();
        RoomEntity newRoom = roomRepository.findById(saved.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        auditLogService.record(
                AuditAction.BOOKING_RELOCATED,
                AuditEntityType.BOOKING,
                saved.getId(),
                "Relocated " + saved.getGuestName() + " from " + result.oldRoom().getName()
                        + (result.oldUnitLabel() != null ? " (" + result.oldUnitLabel() + ")" : "") + " to " + result.newRoom().getName()
                        + (result.newUnit() != null ? " (" + result.newUnit().getLabel() + ")" : "") + ", effective " + effectiveDate);
        return bookingMapper.toDto(saved, newRoom, result.newUnit(), findGuest(saved.getGuestId()), loadSegments(saved.getId()));
    }

    /**
     * Non-mutating preview for {@code POST /bookings/{id}/relocate/quote} - see
     * {@link BookingWriter#quoteRelocation}.
     */
    @Transactional(readOnly = true)
    public BookingScheduleQuote quoteRelocation(String bookingId, RelocationInput input) {
        LocalDate effectiveDate = LocalDate.parse(input.getEffectiveDate());
        String newRoomUnitId = input.getRoomUnitId().isPresent() ? input.getRoomUnitId().get() : null;

        BookingWriter.ScheduleQuote quote = bookingWriter.quoteRelocation(bookingId, effectiveDate, input.getRoomId(), newRoomUnitId);
        return new BookingScheduleQuote(PriceFormat.asDecimalString(quote.totalPrice()), quote.nights(), quote.available(), quote.reason());
    }

    /**
     * Reverses a relocation, merging two segments back into one - the write path behind
     * {@code POST /bookings/{id}/undo-relocation}. Same race-safety story as {@link #relocate}:
     * the earlier room may no longer be free for the range it is about to re-absorb.
     */
    public Booking undoRelocation(String bookingId, RelocationUndoInput input) {
        LocalDate splitDate = LocalDate.parse(input.getSplitDate());

        BookingWriter.RelocationResult result;
        try {
            result = bookingWriter.undoRelocation(bookingId, splitDate);
        } catch (DataAccessException | TransactionSystemException e) {
            if (isSerializationFailure(e)) {
                throw new ConflictException("Someone just booked these dates — please try again.");
            }
            throw e;
        }

        BookingEntity saved = result.booking();
        RoomEntity currentRoom = roomRepository.findById(saved.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        auditLogService.record(
                AuditAction.BOOKING_RELOCATION_UNDONE,
                AuditEntityType.BOOKING,
                saved.getId(),
                "Undid relocation for " + saved.getGuestName() + " — discarded move to " + result.oldRoom().getName()
                        + (result.oldUnitLabel() != null ? " (" + result.oldUnitLabel() + ")" : "") + ", back to " + result.newRoom().getName()
                        + (result.newUnit() != null ? " (" + result.newUnit().getLabel() + ")" : "") + " from " + splitDate);
        return bookingMapper.toDto(saved, currentRoom, result.newUnit(), findGuest(saved.getGuestId()), loadSegments(saved.getId()));
    }

    /**
     * The one explicit, deliberate way to move an already-agreed price forward - the write path
     * behind {@code POST /bookings/{id}/reprice}. See {@link BookingWriter}'s class javadoc
     * ("Nightly price snapshots") for why every other schedule-touching write preserves an
     * already-agreed night's price instead of recomputing it.
     */
    public Booking reprice(String bookingId, RepriceInput input) {
        BookingWriter.RepriceResult result;
        try {
            result = bookingWriter.reprice(bookingId, input.getSegmentId());
        } catch (DataAccessException | TransactionSystemException e) {
            if (isSerializationFailure(e)) {
                throw new ConflictException("Someone just changed this booking — please try again.");
            }
            throw e;
        }

        BookingEntity saved = result.booking();
        RoomEntity room = roomRepository.findById(saved.getRoomId()).orElseThrow(() -> new NotFoundException("Room not found"));
        RoomUnitEntity unit = findRoomUnit(saved.getRoomUnitId());
        auditLogService.record(
                AuditAction.BOOKING_REPRICED,
                AuditEntityType.BOOKING,
                saved.getId(),
                "Repriced " + result.nightsRepriced() + " night(s) for " + saved.getGuestName() + ": ฿" + result.oldSegmentTotal()
                        + " → ฿" + result.newSegmentTotal());
        return bookingMapper.toDto(saved, room, unit, findGuest(saved.getGuestId()), loadSegments(saved.getId()));
    }

    /**
     * Non-mutating preview for {@code POST /bookings/{id}/reprice/quote} - see
     * {@link BookingWriter#quoteReprice}. No serialization-failure handling needed here: this
     * read never writes, so there is nothing for Postgres to report a conflict on.
     */
    @Transactional(readOnly = true)
    public RepriceQuote quoteReprice(String bookingId, RepriceInput input) {
        BookingWriter.RepriceResult result = bookingWriter.quoteReprice(bookingId, input.getSegmentId());
        return new RepriceQuote(
                PriceFormat.asDecimalString(result.oldSegmentTotal()),
                PriceFormat.asDecimalString(result.newSegmentTotal()),
                result.nightsRepriced());
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

    private GuestEntity findGuest(String guestId) {
        return guestId != null ? guestRepository.findById(guestId).orElse(null) : null;
    }

    /**
     * folio(booking) = booking.totalPrice + sum of ROOM_CHARGE Payment.amount for this booking,
     * minus every {@link FolioPaymentEntity} recorded against it (floored at zero) - see
     * {@link #recordFolioPayment}'s javadoc for why a room charge needs its own settlement
     * record at all. Computed on the fly, never stored - this is the one place that calculation
     * is allowed to happen; callers (e.g. {@link #getFolio}, {@link #listPosOrders}) should go
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

    /**
     * What's actually left to collect right now - unlike {@link #computeFolio} (the whole
     * room + room-charges value regardless of what's already been paid), this drops the room
     * portion once {@code status} is {@code PAID}. This system has no separate payment record
     * for the room itself (see {@code RoomChargeDebtBadge}'s own comment for that gap) -
     * {@code PAID} is the only signal it was collected. Uncollected {@code ROOM_CHARGE} payments
     * count either way, since "charged to room" specifically means not yet collected in cash.
     * Used by {@code BookingOccupancyService} at check-out and on the today board - the two
     * places staff need "what do I still need to collect," not "what does this booking add up
     * to."
     */
    @Transactional(readOnly = true)
    public BigDecimal computeOutstandingBalance(String bookingId) {
        FolioBreakdown breakdown = computeFolioBreakdown(bookingId);
        BigDecimal roomOutstanding = breakdown.status() == BookingStatus.PAID ? BigDecimal.ZERO : breakdown.roomTotal();
        return roomOutstanding.add(breakdown.roomChargesTotal());
    }

    /**
     * The one place booking.totalPrice + Σ ROOM_CHARGE Payment.amount, minus Σ FolioPayment, is
     * actually computed. {@code roomChargesTotal} is net of settlement (floored at zero) -
     * without that subtraction, any booking that ever had one room charge would show as owing
     * that charge forever, since nothing ever marked it collected.
     */
    private FolioBreakdown computeFolioBreakdown(String bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        List<PaymentEntity> payments = roomChargePayments(bookingId);
        BigDecimal roomChargesGross = payments.stream().map(PaymentEntity::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal settled = folioPaymentRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream()
                .map(FolioPaymentEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal roomChargesOutstanding = roomChargesGross.subtract(settled).max(BigDecimal.ZERO);
        return new FolioBreakdown(booking.getTotalPrice(), roomChargesOutstanding, payments.size(), booking.getStatus());
    }

    private record FolioBreakdown(BigDecimal roomTotal, BigDecimal roomChargesTotal, int roomChargeCount, BookingStatus status) {
        BigDecimal folioTotal() {
            return roomTotal.add(roomChargesTotal);
        }
    }

    /**
     * Records money actually collected against a booking's ROOM CHARGES specifically - the fix
     * for a real bug: a ROOM_CHARGE {@code Payment} (money charged to the room, meant to be
     * collected later) had no way to ever be marked collected, so any booking that ever had one
     * showed as owing it forever - the check-out warning fired on every check-out regardless of
     * what was actually paid, and {@code RoomChargeDebtBadge} never went dark. Deliberately does
     * not touch {@code Booking.status}/{@code PAID} at all - settling the *room* portion of a
     * stay stays the separate, pre-existing action it already was ({@code PATCH /bookings/{id}}
     * with {@code status: PAID}). A guest settling a mixed room-plus-charges bill needs both
     * actions, not one; that asymmetry is deliberate; see {@code FolioPaymentEntity}'s javadoc
     * for why unifying them wasn't done here. Rejects an amount that would overpay what's
     * currently outstanding on the charges alone - a fat-finger guard, not a restriction on
     * partial payment.
     */
    @Transactional
    public FolioPayment recordFolioPayment(String bookingId, FolioPaymentInput input) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new NotFoundException("Booking not found");
        }
        BigDecimal amount = new BigDecimal(input.getAmount());
        if (amount.signum() <= 0) {
            throw ValidationException.field("amount", "amount must be greater than zero");
        }
        BigDecimal outstanding = computeFolioBreakdown(bookingId).roomChargesTotal();
        if (amount.compareTo(outstanding) > 0) {
            throw new ConflictException(
                    "This would overpay the folio - ฿" + PriceFormat.asDecimalString(outstanding) + " is currently outstanding.");
        }

        StaffPrincipal actor = (StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        FolioPaymentEntity entity = new FolioPaymentEntity();
        entity.setBookingId(bookingId);
        entity.setMethod(input.getMethod());
        entity.setAmount(amount);
        entity.setRecordedByUserId(actor.id());
        FolioPaymentEntity saved = folioPaymentRepository.saveAndFlush(entity);

        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        ledgerService.postFolioPayment(saved, booking.getGuestName());
        auditLogService.record(
                AuditAction.BOOKING_FOLIO_PAYMENT_RECORDED,
                AuditEntityType.BOOKING,
                bookingId,
                "Recorded ฿" + PriceFormat.asDecimalString(amount) + " (" + input.getMethod().getValue() + ") against "
                        + booking.getGuestName() + "'s folio");

        return toFolioPaymentDto(saved);
    }

    /** Settlement history behind {@link #computeFolioBreakdown}'s {@code roomChargesTotal} - see {@link #recordFolioPayment}. */
    @Transactional(readOnly = true)
    public List<FolioPayment> listFolioPayments(String bookingId) {
        if (!bookingRepository.existsById(bookingId)) {
            throw new NotFoundException("Booking not found");
        }
        return folioPaymentRepository.findByBookingIdOrderByCreatedAtAsc(bookingId).stream().map(this::toFolioPaymentDto).toList();
    }

    private FolioPayment toFolioPaymentDto(FolioPaymentEntity entity) {
        return new FolioPayment(
                entity.getId(),
                entity.getBookingId(),
                entity.getMethod(),
                PriceFormat.asDecimalString(entity.getAmount()),
                entity.getRecordedByUserId(),
                TimestampFormat.toUtc(entity.getCreatedAt()));
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
        // guestName search is deliberately not offered on the bulk CSV export - see
        // GET /bookings/export's own description for why that stays a plain date-range report.
        List<BookingEntity> bookings = bookingRepository.findAll(buildSpecification(from, to, status, null, null));
        auditLogService.record(
                AuditAction.BOOKINGS_EXPORTED,
                AuditEntityType.BOOKING,
                null,
                "Exported " + bookings.size() + " booking(s)"
                        + (from != null || to != null ? " for " + (from != null ? from : "…") + " to " + (to != null ? to : "…") : "")
                        + (status != null ? ", status=" + status.getValue() : ""));
        return buildCsv(bookings);
    }

    /**
     * Shared by {@link #list} and {@link #exportCsv} - same filters, same `checkIn` ascending
     * order. {@code guestName} (case-insensitive substring) is passed as {@code null} from
     * {@link #exportCsv} - it's a `GET /bookings`-only filter, see that operation's description.
     *
     * <p>{@code today}, when given, widens the date range by {@link OverstayRule}: a guest still
     * checked in past checkOut is still staying on every date up to today, so a range reaching
     * back to today or earlier includes them - this is what lets POS charge-to-room and the spa's
     * guest picker (both {@code from=to=today}-style lookups) find them. {@code null} keeps the
     * plain agreed-dates filter, for the CSV export (a date-range report, not a who's-here query).
     */
    private static Specification<BookingEntity> buildSpecification(
            String from, String to, BookingStatus status, String guestName, LocalDate today) {
        LocalDate fromDate = from != null ? LocalDate.parse(from) : null;
        LocalDate toDate = to != null ? LocalDate.parse(to) : null;
        String guestNamePattern = guestName != null && !guestName.isBlank() ? "%" + guestName.toLowerCase().trim() + "%" : null;

        return (root, query, cb) -> {
            if (Long.class != query.getResultType()) {
                root.fetch("room");
            }
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (fromDate != null) {
                Predicate staysPastFrom = cb.greaterThan(root.get("checkOut"), fromDate);
                if (today != null && !fromDate.isAfter(today)) {
                    Predicate overdue = cb.and(
                            cb.equal(root.get("occupancyStatus"), OccupancyStatus.CHECKED_IN),
                            cb.notEqual(root.get("status"), BookingStatus.CANCELLED),
                            cb.lessThan(root.get("checkOut"), today));
                    staysPastFrom = cb.or(staysPastFrom, overdue);
                }
                predicates.add(staysPastFrom);
            }
            if (toDate != null) {
                // <=, not < : checkIn/checkOut is a half-open [checkIn, checkOut) stay window,
                // but from/to is a closed [from, to] query range - a booking checking in exactly
                // on `to` (e.g. from=to=today, checkIn=today) must still match. A strict "<"
                // here would silently drop same-day check-ins from range queries.
                predicates.add(cb.lessThanOrEqualTo(root.get("checkIn"), toDate));
            }
            if (guestNamePattern != null) {
                predicates.add(cb.like(cb.lower(root.get("guestName")), guestNamePattern));
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
        return SqlStates.is(ex, SERIALIZATION_FAILURE_SQLSTATE);
    }
}
