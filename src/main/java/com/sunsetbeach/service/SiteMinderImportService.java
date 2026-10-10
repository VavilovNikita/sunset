package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.BookingSource;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.SqlStates;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingChannel;
import com.sunsetbeach.model.BookingScheduleInput;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.BookingStatusInput;
import com.sunsetbeach.model.SiteMinderImportAction;
import com.sunsetbeach.model.SiteMinderImportResult;
import com.sunsetbeach.model.SiteMinderReservationInput;
import com.sunsetbeach.model.SiteMinderReservationStatus;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;

/**
 * One-way SiteMinder -> this system reservation import, behind
 * {@code POST /integrations/siteminder/reservations}. Our polling script reads SiteMinder's
 * reservation screens and posts each reservation here; this class decides whether that means a
 * new booking, a change to one, a cancellation, or nothing - see the operation's openapi.yaml
 * description for the full contract.
 *
 * <p><b>Reuses the staff paths, doesn't write around them.</b> Creation goes through
 * {@link BookingWriter#insertExternal} (same SERIALIZABLE availability check as every insert);
 * a date change through {@link BookingService#updateSchedule}; a party-size change and a
 * cancellation through {@link BookingService#updateStatus} - so a SiteMinder cancellation has
 * exactly the effects of a staff one (room released, a PAID booking's ledger settlement
 * reversed, the same audit row), attributed to the actor {@code SITEMINDER} (see
 * {@code AuditLogService}). Deliberately not {@code @Transactional}: those paths each own their
 * transaction (SERIALIZABLE for the availability-touching ones), and a partial update left by a
 * failing later step converges on the next poll, which re-diffs from the database.
 *
 * <p><b>The one price that isn't ours.</b> Every other booking is priced from this system's own
 * rates and never from a request. An imported booking's price is SiteMinder's total, frozen per
 * night: the guest already agreed it on the OTA, and pricing it from our rates would put a number
 * on the folio and in the ledger that nobody agreed to. It's still validated and frozen the same
 * way (per-night rows, an explicit re-spread only when SiteMinder's own total changes).
 *
 * <p><b>Guests are never matched by name</b> - see
 * {@link GuestLinkService#createNameOnlyCardForBooking}. Every newly imported reservation gets a
 * new name-only card. Duplicate cards for repeat guests are the known cost until SiteMinder's
 * masked contacts become available.
 */
@Service
public class SiteMinderImportService {

    private static final Logger log = LoggerFactory.getLogger(SiteMinderImportService.class);

    private static final String SERIALIZATION_FAILURE_SQLSTATE = "40001";
    private static final String UNIQUE_VIOLATION_SQLSTATE = "23505";

    private final BookingRepository bookingRepository;
    private final BookingSegmentRepository segmentRepository;
    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final BookingWriter bookingWriter;
    private final BookingService bookingService;
    private final GuestLinkService guestLinkService;
    private final SiteMinderRoomTypeMappingService mappingService;
    private final AuditLogService auditLogService;

    public SiteMinderImportService(
            BookingRepository bookingRepository,
            BookingSegmentRepository segmentRepository,
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            BookingWriter bookingWriter,
            BookingService bookingService,
            GuestLinkService guestLinkService,
            SiteMinderRoomTypeMappingService mappingService,
            AuditLogService auditLogService) {
        this.bookingRepository = bookingRepository;
        this.segmentRepository = segmentRepository;
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.bookingWriter = bookingWriter;
        this.bookingService = bookingService;
        this.guestLinkService = guestLinkService;
        this.mappingService = mappingService;
        this.auditLogService = auditLogService;
    }

    public SiteMinderImportResult importReservation(SiteMinderReservationInput input) {
        Reservation r = parse(input);
        BookingEntity booking = bookingRepository.findBySourceAndExternalReference(BookingSource.SITEMINDER, r.reference()).orElse(null);

        if (booking == null) {
            if (r.status() != SiteMinderReservationStatus.BOOKED) {
                log.warn("SiteMinder reservation {} arrived as {} but was never imported - skipped", r.reference(), r.status());
                return skipped(null, "Reservation " + r.reference() + " is " + r.status().getValue()
                        + " in SiteMinder but was never imported here, so there is nothing to " + (r.status() == SiteMinderReservationStatus.CANCELLED ? "cancel" : "modify") + ".");
            }
            return create(r);
        }

        if (booking.getExternalModifiedAt() != null && r.lastChangedAt().isBefore(booking.getExternalModifiedAt())) {
            log.info("SiteMinder reservation {}: import is older than the version already applied - skipped", r.reference());
            return skipped(booking.getId(), "This is an older version of reservation " + r.reference() + " than the one already applied.");
        }
        if (r.status() == SiteMinderReservationStatus.CANCELLED) {
            return cancel(booking, r);
        }
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            log.warn("SiteMinder reservation {} is {} in SiteMinder but its booking {} is cancelled here - not reinstated",
                    r.reference(), r.status(), booking.getId());
            return skipped(booking.getId(), "The booking for reservation " + r.reference() + " is cancelled in this system but "
                    + r.status().getValue() + " in SiteMinder. Imports never reinstate a cancelled booking - a person needs to look at it.");
        }
        return update(booking, r);
    }

    // --- Create --------------------------------------------------------------------------------

    private SiteMinderImportResult create(Reservation r) {
        RoomEntity room = requireMappedRoom(r.roomTypeName());
        BookingEntity saved;
        try {
            saved = bookingWriter.insertExternal(
                    room, r.guestName(), r.checkIn(), r.checkOut(), r.channel(), r.adults(), r.children(), r.totalPrice(),
                    r.reference(), r.rawChannel(), r.lastChangedAt());
        } catch (DataAccessException | TransactionSystemException e) {
            if (SqlStates.is(e, UNIQUE_VIOLATION_SQLSTATE)) {
                throw new ConflictException("Reservation " + r.reference() + " is being imported by another request — try again.");
            }
            if (SqlStates.is(e, SERIALIZATION_FAILURE_SQLSTATE)) {
                throw new ConflictException("Someone just booked these dates — please try again.");
            }
            throw e;
        }

        createGuestCardQuietly(saved);
        auditLogService.record(
                AuditAction.BOOKING_CREATED,
                AuditEntityType.BOOKING,
                saved.getId(),
                "SiteMinder reservation " + r.reference() + " imported for " + saved.getGuestName() + " in " + room.getName() + " ("
                        + r.checkIn() + " to " + r.checkOut() + "), " + r.rawChannel() + ", ฿" + r.totalPrice()
                        + (r.infants() > 0 ? "; " + r.infants() + (r.infants() == 1 ? " infant" : " infants") + " (not stored)" : ""));
        return new SiteMinderImportResult(SiteMinderImportAction.CREATED, saved.getId(), List.of(), List.of(), null);
    }

    /** Same never-break-the-booking rule as {@code BookingService#linkGuestQuietly}: an unlinked booking is still a booking. */
    private void createGuestCardQuietly(BookingEntity booking) {
        try {
            guestLinkService.createNameOnlyCardForBooking(booking.getId());
        } catch (RuntimeException e) {
            log.error("Failed to create a guest card for SiteMinder booking {}", booking.getId(), e);
        }
    }

    // --- Cancel --------------------------------------------------------------------------------

    private SiteMinderImportResult cancel(BookingEntity booking, Reservation r) {
        if (booking.getStatus() == BookingStatus.CANCELLED) {
            // Deliberately no write, not even the timestamp: the manager report counts a day's
            // cancellations by the booking's updatedAt, so touching an already-cancelled booking
            // on every poll would move its cancellation to whatever day the poll ran.
            return unchanged(booking.getId());
        }
        bookingService.updateStatus(booking.getId(), new BookingStatusInput(BookingStatus.CANCELLED));
        recordLastChangedAt(booking.getId(), r.lastChangedAt());
        return new SiteMinderImportResult(SiteMinderImportAction.CANCELLED, booking.getId(), List.of(), List.of(), null);
    }

    // --- Update --------------------------------------------------------------------------------

    private SiteMinderImportResult update(BookingEntity booking, Reservation r) {
        String bookingId = booking.getId();
        RoomEntity room = requireMappedRoom(r.roomTypeName());
        List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(bookingId);
        BookingSegmentEntity first = segments.get(0);
        // The first segment's room is what was booked - a relocation by staff since then moves
        // later legs, never the first, so this doesn't mistake a staff room move for a change.
        if (!first.getRoomId().equals(room.getId())) {
            String bookedName = roomRepository.findById(first.getRoomId()).map(RoomEntity::getName).orElse(first.getRoomId());
            throw new ConflictException("SiteMinder now shows room type \"" + r.roomTypeName() + "\" (" + room.getName()
                    + ") for reservation " + r.reference() + ", but this booking was made for " + bookedName
                    + ". Changing a whole stay's room type isn't automated - move the guest by hand.");
        }

        List<String> changes = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        boolean datesChange = !r.checkIn().equals(booking.getCheckIn()) || !r.checkOut().equals(booking.getCheckOut());
        if (datesChange) {
            changeDates(booking, segments, r, warnings);
            changes.add("dates " + booking.getCheckIn() + " – " + booking.getCheckOut() + " → " + r.checkIn() + " – " + r.checkOut());
        }

        // A date change prices any added night at this system's rate (that's updateSchedule's
        // rule for a staff extension), so the agreed total is re-spread over the new nights even
        // when the sum happens to match - SiteMinder's total, not our rate, is what was agreed.
        BookingEntity current = bookingRepository.findById(bookingId).orElseThrow();
        boolean totalChanges = current.getTotalPrice().compareTo(r.totalPrice()) != 0;
        if (datesChange && !totalChanges && segments.size() == 1) {
            bookingWriter.applyAgreedTotal(bookingId, r.totalPrice());
        }
        if (totalChanges) {
            if (segments.size() > 1) {
                warnings.add("SiteMinder's total ฿" + r.totalPrice() + " differs from this booking's ฿" + current.getTotalPrice()
                        + ", but the booking has been split by a relocation, so the price wasn't changed - adjust it by hand.");
            } else {
                BigDecimal oldTotal = current.getTotalPrice();
                bookingWriter.applyAgreedTotal(bookingId, r.totalPrice());
                auditLogService.record(
                        AuditAction.BOOKING_REPRICED,
                        AuditEntityType.BOOKING,
                        bookingId,
                        "SiteMinder total changed for " + current.getGuestName() + ": ฿" + oldTotal + " → ฿" + r.totalPrice());
                changes.add("total ฿" + oldTotal + " → ฿" + r.totalPrice());
                if (current.getStatus() == BookingStatus.PAID) {
                    warnings.add("The booking is PAID, so this price change isn't posted to the ledger - same as any price change on a PAID booking.");
                }
            }
        }

        if (current.getAdults() != r.adults() || current.getChildren() != r.children()) {
            BookingStatusInput guests = new BookingStatusInput(current.getStatus()).adults(r.adults()).children(r.children());
            bookingService.updateStatus(bookingId, guests);
            changes.add("guests " + current.getAdults() + "+" + current.getChildren() + " → " + r.adults() + "+" + r.children());
        }

        recordLastChangedAt(bookingId, r.lastChangedAt());
        if (changes.isEmpty()) {
            return new SiteMinderImportResult(SiteMinderImportAction.UNCHANGED, bookingId, List.of(), warnings, null);
        }
        return new SiteMinderImportResult(SiteMinderImportAction.UPDATED, bookingId, changes, warnings, null);
    }

    /**
     * Through {@link BookingService#updateSchedule}, keeping the physical room assigned to the
     * leg that moves - unless that room isn't free for the new dates while the room type still
     * is, in which case the room is unassigned (with a warning) rather than failing the whole
     * change: the reservation exists at the OTA either way, and picking a room is a front-desk job.
     */
    private void changeDates(BookingEntity booking, List<BookingSegmentEntity> segments, Reservation r, List<String> warnings) {
        boolean onlyCheckInMoves = !r.checkIn().equals(booking.getCheckIn()) && r.checkOut().equals(booking.getCheckOut());
        BookingSegmentEntity movingLeg = onlyCheckInMoves ? segments.get(0) : segments.get(segments.size() - 1);
        String unitId = movingLeg.getRoomUnitId();
        if (unitId != null
                && !Boolean.TRUE.equals(bookingService.quoteSchedule(booking.getId(), scheduleInput(r, unitId)).getAvailable())
                && Boolean.TRUE.equals(bookingService.quoteSchedule(booking.getId(), scheduleInput(r, null)).getAvailable())) {
            String label = roomUnitRepository.findById(unitId).map(RoomUnitEntity::getLabel).orElse(unitId);
            warnings.add("Room " + label + " isn't free for the new dates, so it was unassigned - assign a room again.");
            unitId = null;
        }
        bookingService.updateSchedule(booking.getId(), scheduleInput(r, unitId));
    }

    private static BookingScheduleInput scheduleInput(Reservation r, String roomUnitId) {
        return new BookingScheduleInput(r.checkIn().toString(), r.checkOut().toString()).roomUnitId(roomUnitId);
    }

    /** Only ever moves forward, and only writes when it actually moves - see {@link #cancel} for why a needless write matters. */
    private void recordLastChangedAt(String bookingId, LocalDateTime lastChangedAt) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow();
        if (booking.getExternalModifiedAt() == null || lastChangedAt.isAfter(booking.getExternalModifiedAt())) {
            booking.setExternalModifiedAt(lastChangedAt);
            bookingRepository.saveAndFlush(booking);
        }
    }

    // --- Input ---------------------------------------------------------------------------------

    private RoomEntity requireMappedRoom(String siteMinderRoomType) {
        return mappingService.resolveRoom(siteMinderRoomType).orElseThrow(() -> ValidationException.field(
                "roomTypeName",
                "SiteMinder room type \"" + siteMinderRoomType + "\" isn't mapped to a room type - add it under SiteMinder room type mappings."));
    }

    /** The request, validated and normalized. */
    private record Reservation(
            String reference,
            SiteMinderReservationStatus status,
            String guestName,
            LocalDate checkIn,
            LocalDate checkOut,
            String roomTypeName,
            int adults,
            int children,
            int infants,
            BigDecimal totalPrice,
            BookingChannel channel,
            String rawChannel,
            LocalDateTime lastChangedAt) {
    }

    private static Reservation parse(SiteMinderReservationInput input) {
        LocalDate checkIn = LocalDate.parse(input.getCheckIn());
        LocalDate checkOut = LocalDate.parse(input.getCheckOut());
        if (!checkIn.isBefore(checkOut)) {
            throw ValidationException.field("checkOut", "checkIn must be before checkOut");
        }
        if (input.getCurrency() != null && !input.getCurrency().trim().equalsIgnoreCase("THB")) {
            throw ValidationException.field("currency", "Only THB is supported - got \"" + input.getCurrency() + "\"");
        }
        String firstName = input.getFirstName() != null ? input.getFirstName().trim() : "";
        String guestName = (firstName + " " + input.getLastName().trim()).trim().replaceAll("\\s+", " ");
        if (guestName.isEmpty()) {
            throw ValidationException.field("lastName", "A guest name is required");
        }
        String rawChannel = input.getChannel().trim();
        OffsetDateTime lastChanged = Stream.of(input.getBookedAt(), input.getModifiedAt(), input.getCancelledAt())
                .filter(Objects::nonNull)
                .max(OffsetDateTime::compareTo)
                .orElseThrow();
        return new Reservation(
                input.getReference().trim(),
                input.getStatus(),
                guestName,
                checkIn,
                checkOut,
                input.getRoomTypeName(),
                input.getAdults(),
                input.getChildren() != null ? input.getChildren() : 0,
                input.getInfants() != null ? input.getInfants() : 0,
                new BigDecimal(input.getTotalPrice()).setScale(2, RoundingMode.UNNECESSARY),
                mapChannel(rawChannel),
                rawChannel,
                lastChanged.withOffsetSameInstant(ZoneOffset.UTC).toLocalDateTime());
    }

    /**
     * SiteMinder's channel name -> {@link BookingChannel}. Only channels that already have a value
     * get one; everything else is {@code OTHER}, with the raw name kept on the booking
     * ({@code externalChannel}) so it can be reclassified if a dedicated value is added later.
     * SiteMinder's own booking engine is the hotel's website, so it's {@code DIRECT}.
     */
    static BookingChannel mapChannel(String rawChannel) {
        String n = rawChannel.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        if (n.startsWith("bookingcom")) {
            return BookingChannel.BOOKING_COM;
        }
        if (n.startsWith("expedia")) {
            return BookingChannel.EXPEDIA;
        }
        if (n.startsWith("agoda")) {
            return BookingChannel.AGODA;
        }
        if (n.startsWith("airbnb")) {
            return BookingChannel.AIRBNB;
        }
        if (n.equals("direct") || n.contains("bookingengine") || n.equals("thebookingbutton")) {
            return BookingChannel.DIRECT;
        }
        return BookingChannel.OTHER;
    }

    private static SiteMinderImportResult skipped(String bookingId, String message) {
        return new SiteMinderImportResult(SiteMinderImportAction.SKIPPED, bookingId, List.of(), List.of(), message);
    }

    private static SiteMinderImportResult unchanged(String bookingId) {
        return new SiteMinderImportResult(SiteMinderImportAction.UNCHANGED, bookingId, List.of(), List.of(), null);
    }
}
