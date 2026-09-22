package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.model.GuestAccount;
import com.sunsetbeach.model.GuestBookingView;
import com.sunsetbeach.repository.RoomRepository;
import java.time.ZoneOffset;
import org.springframework.stereotype.Component;

@Component
public class GuestAccountMapper {

    private final RoomRepository roomRepository;

    public GuestAccountMapper(RoomRepository roomRepository) {
        this.roomRepository = roomRepository;
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
     */
    public GuestBookingView toGuestBookingView(BookingEntity entity) {
        String roomName = roomRepository.findById(entity.getRoomId()).map(RoomEntity::getName).orElse(entity.getRoomId());
        GuestBookingView dto = new GuestBookingView(
                entity.getId(),
                roomName,
                entity.getCheckIn().toString(),
                entity.getCheckOut().toString(),
                entity.getTotalPrice().toString(),
                entity.getStatus(),
                entity.getCreatedAt().atOffset(ZoneOffset.UTC));
        return dto;
    }
}
