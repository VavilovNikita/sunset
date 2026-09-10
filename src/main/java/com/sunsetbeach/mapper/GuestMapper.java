package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.model.Booking;
import com.sunsetbeach.model.Guest;
import com.sunsetbeach.model.GuestCreateInput;
import com.sunsetbeach.model.GuestDetail;
import com.sunsetbeach.model.GuestUpdateInput;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class GuestMapper {

    public Guest toDto(GuestEntity entity) {
        return new Guest(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getNotes(),
                TimestampFormat.toUtc(entity.getCreatedAt()),
                TimestampFormat.toUtc(entity.getUpdatedAt()));
    }

    /** {@code bookings} must already be this guest's full stay history, newest first - see {@code GuestDetail}'s own description. */
    public GuestDetail toDetailDto(GuestEntity entity, List<Booking> bookings) {
        return new GuestDetail(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getNotes(),
                TimestampFormat.toUtc(entity.getCreatedAt()),
                TimestampFormat.toUtc(entity.getUpdatedAt()),
                bookings);
    }

    public void applyCreate(GuestEntity entity, GuestCreateInput input) {
        entity.setName(input.getName().trim());
        entity.setEmail(input.getEmail().isPresent() ? blankToNull(input.getEmail().get()) : null);
        entity.setPhone(input.getPhone().isPresent() ? blankToNull(input.getPhone().get()) : null);
        entity.setNotes(input.getNotes().isPresent() ? blankToNull(input.getNotes().get()) : null);
    }

    public void applyUpdate(GuestEntity entity, GuestUpdateInput input) {
        entity.setName(input.getName().trim());
        entity.setEmail(input.getEmail().isPresent() ? blankToNull(input.getEmail().get()) : null);
        entity.setPhone(input.getPhone().isPresent() ? blankToNull(input.getPhone().get()) : null);
        entity.setNotes(input.getNotes().isPresent() ? blankToNull(input.getNotes().get()) : null);
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
