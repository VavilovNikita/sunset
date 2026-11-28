package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.BookingSegmentEntity;
import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.GuestAccount;
import com.sunsetbeach.model.GuestBookingView;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.repository.BookingSegmentRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.service.OverstayRule;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GuestAccountMapper {

    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final BookingSegmentRepository segmentRepository;
    private final Clock clock;

    public GuestAccountMapper(
            RoomRepository roomRepository, RoomUnitRepository roomUnitRepository, BookingSegmentRepository segmentRepository, Clock clock) {
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.segmentRepository = segmentRepository;
        this.clock = clock;
    }

    public GuestAccount toDto(GuestAccountEntity entity) {
        GuestAccount dto = new GuestAccount(entity.getId(), entity.getEmail(), entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        dto.name(entity.getName());
        if (entity.getEmailVerifiedAt() != null) {
            dto.emailVerifiedAt(entity.getEmailVerifiedAt().atOffset(ZoneOffset.UTC));
        }
        return dto;
    }

    /**
     * roomName is resolved live at read time, not stored - same "denormalized, not frozen"
     * convention as SpaAppointmentTreatment.currentPrice (see CLAUDE.md's Spa billing section):
     * a room type's display name is cosmetic, not an agreed term the way a price/duration is.
     * Falls back to the raw roomId if the room type was since deleted, rather than failing the
     * whole booking-history read over one stale reference.
     *
     * <p>For a guest in the house the room (and its type) is the one they are in today
     * ({@link OverstayRule#unitOn}); the booking's own is the last segment's, which a planned move
     * has not reached. Any other booking is described by its own room.
     */
    public GuestBookingView toGuestBookingView(BookingEntity entity) {
        String roomId = entity.getRoomId();
        String roomUnitId = entity.getRoomUnitId();
        if (entity.getOccupancyStatus() == OccupancyStatus.CHECKED_IN) {
            LocalDate today = LocalDate.now(clock);
            List<BookingSegmentEntity> segments = segmentRepository.findByBookingIdOrderByCheckInAsc(entity.getId());
            BookingSegmentEntity current = OverstayRule.roomSegmentOn(entity, segments, today, clock);
            if (current != null) {
                roomId = current.getRoomId();
            }
            roomUnitId = OverstayRule.unitOn(entity, segments, today, clock);
        }
        String roomName = roomRepository.findById(roomId).map(RoomEntity::getName).orElse(roomId);
        String roomLabel = roomUnitId == null
                ? null
                : roomUnitRepository.findById(roomUnitId).map(RoomUnitEntity::getLabel).orElse(null);
        GuestBookingView dto = new GuestBookingView(
                entity.getId(),
                roomName,
                roomLabel,
                entity.getCheckIn().toString(),
                entity.getCheckOut().toString(),
                entity.getTotalPrice().toString(),
                entity.getStatus(),
                entity.getOccupancyStatus(),
                entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        return dto;
    }
}
