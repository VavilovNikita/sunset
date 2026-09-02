package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.PropertyMapEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.PriceFormat;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.PropertyMap;
import com.sunsetbeach.model.PropertyMapActiveBlock;
import com.sunsetbeach.model.PropertyMapCurrentBooking;
import com.sunsetbeach.model.PropertyMapUnit;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.PropertyMapRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * The front desk's visual map of the property - see the PropertyMap tag description in
 * openapi.yaml for what this screen is for and how it relates to TodayBoard/the booking
 * calendar. Reuses, rather than re-derives: {@link BookingService#computeOutstandingBalance} for
 * the debt figure, {@link ImageUploadValidator} for the upload mechanism (same as
 * {@code RoomService.uploadImages}), and {@code BookingRepository}'s existing occupancy queries
 * (the same ones {@code BookingOccupancyService#getTodayBoard} uses) rather than new bespoke
 * repository methods.
 */
@Service
public class PropertyMapService {

    // Single row - see V29__property_map.sql.
    private static final String SINGLETON_ID = "default";

    private final RoomUnitRepository roomUnitRepository;
    private final RoomRepository roomRepository;
    private final RoomUnitBlockRepository roomUnitBlockRepository;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final PropertyMapRepository propertyMapRepository;
    private final ImageUploadValidator imageUploadValidator;
    private final AuditLogService auditLogService;
    private final Path uploadsRoot;

    public PropertyMapService(
            RoomUnitRepository roomUnitRepository,
            RoomRepository roomRepository,
            RoomUnitBlockRepository roomUnitBlockRepository,
            BookingRepository bookingRepository,
            BookingService bookingService,
            PropertyMapRepository propertyMapRepository,
            ImageUploadValidator imageUploadValidator,
            AuditLogService auditLogService,
            @Value("${app.uploads.root}") String uploadsRoot) {
        this.roomUnitRepository = roomUnitRepository;
        this.roomRepository = roomRepository;
        this.roomUnitBlockRepository = roomUnitBlockRepository;
        this.bookingRepository = bookingRepository;
        this.bookingService = bookingService;
        this.propertyMapRepository = propertyMapRepository;
        this.imageUploadValidator = imageUploadValidator;
        this.auditLogService = auditLogService;
        this.uploadsRoot = Path.of(uploadsRoot);
    }

    @Transactional(readOnly = true)
    public PropertyMap get() {
        List<RoomUnitEntity> units = roomUnitRepository.findAll();
        Map<String, String> roomNames = roomRepository.findAll().stream().collect(Collectors.toMap(RoomEntity::getId, RoomEntity::getName));

        List<String> unitIds = units.stream().map(RoomUnitEntity::getId).toList();
        LocalDate today = LocalDate.now();

        // Currently in-house (a real guest, right now) - same query BookingOccupancyService#getTodayBoard
        // uses for its own inHouse list, just regrouped by roomUnitId instead of returned as a flat list.
        Map<String, BookingEntity> checkedInByUnit = bookingRepository
                .findByOccupancyStatusAndStatusNot(OccupancyStatus.CHECKED_IN, BookingStatus.CANCELLED).stream()
                .filter(b -> b.getRoomUnitId() != null)
                .collect(Collectors.toMap(BookingEntity::getRoomUnitId, b -> b, (a, b) -> a));

        // Expected to check in today, not checked in yet - only shown for a unit that isn't
        // already occupied (see toUnitDto): a unit can't show two "current" bookings at once.
        Map<String, BookingEntity> arrivingTodayByUnit = bookingRepository
                .findByOccupancyStatusAndStatusNotAndCheckInIs(OccupancyStatus.EXPECTED, BookingStatus.CANCELLED, today).stream()
                .filter(b -> b.getRoomUnitId() != null)
                .collect(Collectors.toMap(BookingEntity::getRoomUnitId, b -> b, (a, b) -> a));

        // Same repository method + single-day-range trick AvailabilityService#computeInventory
        // already uses to find blocks covering one date - no new query needed.
        List<RoomUnitBlockEntity> blocksToday = unitIds.isEmpty()
                ? List.of()
                : roomUnitBlockRepository.findByRoomUnitIdInAndFromDateLessThanEqualAndToDateGreaterThanEqual(unitIds, today, today);
        Map<String, RoomUnitBlockEntity> blockByUnit =
                blocksToday.stream().collect(Collectors.toMap(RoomUnitBlockEntity::getRoomUnitId, b -> b, (a, b) -> a));

        List<PropertyMapUnit> unitDtos =
                units.stream().map(unit -> toUnitDto(unit, roomNames, checkedInByUnit, arrivingTodayByUnit, blockByUnit)).toList();

        PropertyMapEntity map = propertyMapRepository.findById(SINGLETON_ID).orElse(null);
        String imagePath = map != null ? map.getImagePath() : null;
        OffsetDateTime imageUpdatedAt = map != null ? TimestampFormat.toUtc(map.getUpdatedAt()) : null;

        return new PropertyMap(imagePath, imageUpdatedAt, unitDtos);
    }

    private PropertyMapUnit toUnitDto(
            RoomUnitEntity unit,
            Map<String, String> roomNames,
            Map<String, BookingEntity> checkedInByUnit,
            Map<String, BookingEntity> arrivingTodayByUnit,
            Map<String, RoomUnitBlockEntity> blockByUnit) {
        BookingEntity checkedIn = checkedInByUnit.get(unit.getId());
        PropertyMapCurrentBooking currentBooking =
                checkedIn != null ? toCurrentBookingDto(checkedIn) : toCurrentBookingDtoOrNull(arrivingTodayByUnit.get(unit.getId()));

        RoomUnitBlockEntity block = blockByUnit.get(unit.getId());
        PropertyMapActiveBlock activeBlock =
                block == null ? null : new PropertyMapActiveBlock(block.getReason(), block.getFromDate().toString(), block.getToDate().toString());

        return new PropertyMapUnit(
                unit.getId(),
                unit.getRoomId(),
                roomNames.getOrDefault(unit.getRoomId(), ""),
                unit.getLabel(),
                unit.isActive(),
                unit.getHousekeepingStatus(),
                unit.getPositionX(),
                unit.getPositionY(),
                currentBooking,
                activeBlock);
    }

    private PropertyMapCurrentBooking toCurrentBookingDtoOrNull(BookingEntity booking) {
        return booking == null ? null : toCurrentBookingDto(booking);
    }

    private PropertyMapCurrentBooking toCurrentBookingDto(BookingEntity booking) {
        BigDecimal outstanding = bookingService.computeOutstandingBalance(booking.getId());
        return new PropertyMapCurrentBooking(
                booking.getId(),
                booking.getGuestName(),
                booking.getCheckOut().toString(),
                booking.getOccupancyStatus(),
                PriceFormat.asDecimalString(outstanding));
    }

    @Transactional
    public PropertyMap uploadImage(MultipartFile file) {
        ImageUploadValidator.ValidatedImage validated = imageUploadValidator.validate(file);

        Path dir = uploadsRoot.resolve("property-map");
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new IllegalStateException("Could not create upload directory", e);
        }
        String filename = imageUploadValidator.randomFilename(validated.extension());
        try {
            Files.write(dir.resolve(filename), validated.bytes());
        } catch (IOException e) {
            throw new IllegalStateException("Could not write uploaded file", e);
        }

        PropertyMapEntity entity = propertyMapRepository.findById(SINGLETON_ID).orElseGet(PropertyMapEntity::new);
        entity.setId(SINGLETON_ID);
        String oldFilename = entity.getImagePath();
        StaffPrincipal actor = (StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        entity.setImagePath(filename);
        entity.setUpdatedByUserId(actor.id());
        propertyMapRepository.saveAndFlush(entity);

        // Best-effort: replacing the plan must never fail because the old file couldn't be
        // removed - matches RoomService.deleteImage's own best-effort disk cleanup.
        if (oldFilename != null) {
            try {
                Files.deleteIfExists(dir.resolve(oldFilename));
            } catch (IOException ignored) {
                // best-effort
            }
        }

        auditLogService.record(AuditAction.PROPERTY_MAP_IMAGE_UPDATED, AuditEntityType.PROPERTY_MAP, SINGLETON_ID, "Property map background image replaced");

        return get();
    }

    @Transactional(readOnly = true)
    public Resource resolveImage() {
        PropertyMapEntity entity =
                propertyMapRepository.findById(SINGLETON_ID).orElseThrow(() -> new NotFoundException("No property map image has been uploaded yet"));

        Path dir = uploadsRoot.resolve("property-map").toAbsolutePath().normalize();
        Path file = dir.resolve(entity.getImagePath()).normalize();
        if (!file.startsWith(dir) || !Files.isRegularFile(file)) {
            throw new NotFoundException("No property map image has been uploaded yet");
        }

        try {
            Resource resource = new UrlResource(file.toUri());
            if (!resource.exists() || !resource.isReadable()) {
                throw new NotFoundException("No property map image has been uploaded yet");
            }
            return resource;
        } catch (MalformedURLException e) {
            throw new NotFoundException("No property map image has been uploaded yet");
        }
    }
}
