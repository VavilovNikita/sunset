package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.RoomUnit;
import com.sunsetbeach.model.RoomUnitBlock;
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

    public RoomUnitBlock toDto(RoomUnitBlockEntity entity) {
        return new RoomUnitBlock(
                entity.getId(),
                entity.getRoomUnitId(),
                entity.getFromDate().toString(),
                entity.getToDate().toString(),
                entity.getReason(),
                TimestampFormat.toUtc(entity.getCreatedAt()));
    }
}
