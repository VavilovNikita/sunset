package com.sunsetbeach.controller;

import com.sunsetbeach.api.RoomsApi;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.model.DeleteRoomImageRequest;
import com.sunsetbeach.model.OkTrue;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomInput;
import com.sunsetbeach.service.RoomService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
public class RoomController implements RoomsApi {

    private final RoomService roomService;

    public RoomController(RoomService roomService) {
        this.roomService = roomService;
    }

    @Override
    public ResponseEntity<Room> createRoom(RoomInput roomInput) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.create(roomInput));
    }

    @Override
    public ResponseEntity<Room> updateRoom(String id, RoomInput roomInput) {
        return ResponseEntity.ok(roomService.update(id, roomInput));
    }

    @Override
    public ResponseEntity<OkTrue> deleteRoom(String id) {
        roomService.delete(id);
        return ResponseEntity.ok(new OkTrue(true));
    }

    @Override
    public ResponseEntity<Room> uploadRoomImages(String id, List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(roomService.uploadImages(id, files));
    }

    @Override
    public ResponseEntity<Room> deleteRoomImage(String id, DeleteRoomImageRequest deleteRoomImageRequest) {
        String path = deleteRoomImageRequest.getPath();
        if (path == null || path.isEmpty()) {
            throw new BadRequestException("path is required");
        }
        return ResponseEntity.ok(roomService.deleteImage(id, path));
    }
}
