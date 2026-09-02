package com.sunsetbeach.controller;

import com.sunsetbeach.api.RoomUnitsApi;
import com.sunsetbeach.model.HousekeepingStatusInput;
import com.sunsetbeach.model.OkTrue;
import com.sunsetbeach.model.RoomUnit;
import com.sunsetbeach.model.RoomUnitBlock;
import com.sunsetbeach.model.RoomUnitBlockInput;
import com.sunsetbeach.model.RoomUnitInput;
import com.sunsetbeach.model.RoomUnitUpdateInput;
import com.sunsetbeach.service.RoomUnitService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RoomUnitController implements RoomUnitsApi {

    private final RoomUnitService roomUnitService;

    public RoomUnitController(RoomUnitService roomUnitService) {
        this.roomUnitService = roomUnitService;
    }

    @Override
    public ResponseEntity<List<RoomUnit>> listRoomUnits(String roomId) {
        return ResponseEntity.ok(roomUnitService.list(roomId));
    }

    @Override
    public ResponseEntity<RoomUnit> getRoomUnit(String id) {
        return ResponseEntity.ok(roomUnitService.getById(id));
    }

    @Override
    public ResponseEntity<RoomUnit> createRoomUnit(RoomUnitInput roomUnitInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomUnitService.create(roomUnitInput));
    }

    @Override
    public ResponseEntity<RoomUnit> updateRoomUnit(String id, RoomUnitUpdateInput roomUnitUpdateInput) {
        return ResponseEntity.ok(roomUnitService.update(id, roomUnitUpdateInput));
    }

    @Override
    public ResponseEntity<RoomUnit> updateRoomUnitHousekeeping(String id, HousekeepingStatusInput housekeepingStatusInput) {
        return ResponseEntity.ok(roomUnitService.updateHousekeeping(id, housekeepingStatusInput.getHousekeepingStatus()));
    }

    @Override
    public ResponseEntity<OkTrue> deleteRoomUnit(String id) {
        roomUnitService.delete(id);
        return ResponseEntity.ok(new OkTrue(true));
    }

    @Override
    public ResponseEntity<List<RoomUnitBlock>> listRoomUnitBlocks(String id) {
        return ResponseEntity.ok(roomUnitService.listBlocks(id));
    }

    @Override
    public ResponseEntity<RoomUnitBlock> createRoomUnitBlock(String id, RoomUnitBlockInput roomUnitBlockInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomUnitService.createBlock(id, roomUnitBlockInput));
    }

    @Override
    public ResponseEntity<OkTrue> deleteRoomUnitBlock(String id, String blockId) {
        roomUnitService.deleteBlock(id, blockId);
        return ResponseEntity.ok(new OkTrue(true));
    }
}
