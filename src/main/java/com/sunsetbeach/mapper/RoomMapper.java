package com.sunsetbeach.mapper;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomInput;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class RoomMapper {

    private final RoomUnitRepository roomUnitRepository;

    public RoomMapper(RoomUnitRepository roomUnitRepository) {
        this.roomUnitRepository = roomUnitRepository;
    }

    /**
     * One extra query per call for {@code activeUnitCount} - accepted N+1 rather than requiring
     * every caller to thread a bulk-fetched count through, matching this project's existing
     * "small hotel, no pagination" scale (see {@code RoomService.list()}). Callers that render
     * many rooms at once (e.g. {@code RoomService.list()}) should prefer {@link #toDto(RoomEntity, long)}
     * with a pre-fetched count instead.
     */
    public Room toDto(RoomEntity entity) {
        return toDto(entity, roomUnitRepository.countByRoomIdAndIsActiveTrue(entity.getId()));
    }

    public Room toDto(RoomEntity entity, long activeUnitCount) {
        return new Room(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCapacity(),
                (int) activeUnitCount,
                PriceFormat.asDecimalString(entity.getBasePrice()),
                List.copyOf(Arrays.asList(entity.getImages())),
                TimestampFormat.toUtc(entity.getCreatedAt()));
    }

    /** roomSchema is a full replacement on both create and update - applies every field except the unit count, which is computed. */
    public void applyInput(RoomEntity entity, RoomInput input) {
        entity.setName(input.getName().trim());
        entity.setDescription(input.getDescription().trim());
        entity.setCapacity(input.getCapacity());
        entity.setBasePrice(input.getBasePrice());
    }
}
