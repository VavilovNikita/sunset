package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomInput;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

    public Room toDto(RoomEntity entity) {
        return new Room(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCapacity(),
                entity.getQuantity(),
                PriceFormat.asDecimalString(entity.getBasePrice()),
                List.copyOf(Arrays.asList(entity.getImages())),
                TimestampFormat.toUtc(entity.getCreatedAt()));
    }

    /** roomSchema is a full replacement on both create and update - applies every field. */
    public void applyInput(RoomEntity entity, RoomInput input) {
        entity.setName(input.getName().trim());
        entity.setDescription(input.getDescription().trim());
        entity.setCapacity(input.getCapacity());
        entity.setQuantity(input.getQuantity());
        entity.setBasePrice(input.getBasePrice());
    }
}
