package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.RoomUnit;
import com.sunsetbeach.model.RoomUnitBlock;
import com.sunsetbeach.model.RoomUnitBlockMaintenanceTask;
import com.sunsetbeach.model.RoomUnitInput;
import com.sunsetbeach.model.RoomUnitUpdateInput;
import org.springframework.stereotype.Component;

@Component
public class RoomUnitMapper {

    public RoomUnit toDto(RoomUnitEntity entity) {
        return new RoomUnit(
                entity.getId(),
                entity.getRoomId(),
                entity.getLabel(),
                entity.isActive(),
                entity.getHousekeepingStatus(),
                TimestampFormat.toUtc(entity.getCreatedAt()))
                .positionX(entity.getPositionX())
                .positionY(entity.getPositionY());
    }

    public void applyInput(RoomUnitEntity entity, RoomUnitInput input) {
        entity.setRoomId(input.getRoomId());
        entity.setLabel(input.getLabel().trim());
        entity.setActive(input.getIsActive() == null || input.getIsActive());
    }

    public void applyUpdate(RoomUnitEntity entity, RoomUnitUpdateInput input) {
        entity.setLabel(input.getLabel().trim());
        entity.setActive(input.getIsActive());
    }

    /**
     * {@code createdByEmail}/{@code maintenanceTask} are resolved by the caller, not here - this
     * mapper stays free of repository dependencies, and both facts need a batched lookup across
     * however many blocks are being mapped at once ({@link com.sunsetbeach.service.BookingCalendarService}'s
     * own read), not a query per block. Pass {@code null} for either when there's nothing to show
     * (a freshly created block never has a task yet - see {@code RoomUnitService#createBlock}).
     */
    public RoomUnitBlock toDto(RoomUnitBlockEntity entity, String createdByEmail, RoomUnitBlockMaintenanceTask maintenanceTask) {
        return new RoomUnitBlock(
                entity.getId(),
                entity.getRoomUnitId(),
                entity.getFromDate().toString(),
                entity.getToDate().toString(),
                entity.getReason(),
                TimestampFormat.toUtc(entity.getCreatedAt()),
                createdByEmail,
                maintenanceTask);
    }
}
