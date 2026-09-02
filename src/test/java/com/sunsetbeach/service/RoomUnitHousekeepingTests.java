package com.sunsetbeach.service;
import com.sunsetbeach.AbstractIntegrationTest;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.model.HousekeepingStatus;
import com.sunsetbeach.model.RoomUnit;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Housekeeping status - independent of RoomUnitBlock (which pulls a unit off sale for a written
 * reason) - see V24__room_unit_housekeeping_status.sql and RoomUnitService#updateHousekeeping.
 */
@SpringBootTest
@Transactional
class RoomUnitHousekeepingTests extends AbstractIntegrationTest {

    @Autowired
    private RoomUnitService roomUnitService;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomUnitRepository roomUnitRepository;

    private RoomUnitEntity createUnit() {
        RoomEntity room = new RoomEntity();
        room.setName("Housekeeping Test Room " + UUID.randomUUID());
        room.setDescription("Room used only by RoomUnitHousekeepingTests");
        room.setCapacity(2);
        room.setBasePrice(new BigDecimal("1000.00"));
        RoomEntity savedRoom = roomRepository.saveAndFlush(room);

        RoomUnitEntity unit = new RoomUnitEntity();
        unit.setRoomId(savedRoom.getId());
        unit.setLabel("Housekeeping Test Unit " + UUID.randomUUID());
        unit.setActive(true);
        return roomUnitRepository.saveAndFlush(unit);
    }

    @Test
    void newUnit_defaultsToClean() {
        RoomUnitEntity unit = createUnit();
        assertThat(unit.getHousekeepingStatus()).isEqualTo(HousekeepingStatus.CLEAN);
    }

    @Test
    void updateHousekeeping_marksDirtyThenClean() {
        RoomUnitEntity unit = createUnit();

        RoomUnit dirty = roomUnitService.updateHousekeeping(unit.getId(), HousekeepingStatus.DIRTY);
        assertThat(dirty.getHousekeepingStatus()).isEqualTo(HousekeepingStatus.DIRTY);
        assertThat(roomUnitRepository.findById(unit.getId()).orElseThrow().getHousekeepingStatus()).isEqualTo(HousekeepingStatus.DIRTY);

        RoomUnit clean = roomUnitService.updateHousekeeping(unit.getId(), HousekeepingStatus.CLEAN);
        assertThat(clean.getHousekeepingStatus()).isEqualTo(HousekeepingStatus.CLEAN);
        assertThat(roomUnitRepository.findById(unit.getId()).orElseThrow().getHousekeepingStatus()).isEqualTo(HousekeepingStatus.CLEAN);
    }

    @Test
    void updateHousekeeping_isIndependentOfLabelAndActiveFields() {
        RoomUnitEntity unit = createUnit();
        String originalLabel = unit.getLabel();

        RoomUnit updated = roomUnitService.updateHousekeeping(unit.getId(), HousekeepingStatus.DIRTY);

        assertThat(updated.getLabel()).isEqualTo(originalLabel);
        assertThat(updated.getIsActive()).isTrue();
    }
}
