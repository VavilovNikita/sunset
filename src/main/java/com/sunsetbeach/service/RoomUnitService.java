package com.sunsetbeach.service;

import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.RoomUnitMapper;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.RoomUnit;
import com.sunsetbeach.model.RoomUnitBlock;
import com.sunsetbeach.model.RoomUnitBlockInput;
import com.sunsetbeach.model.RoomUnitInput;
import com.sunsetbeach.model.RoomUnitUpdateInput;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.RoomRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomUnitService {

    private final RoomRepository roomRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final RoomUnitBlockRepository roomUnitBlockRepository;
    private final BookingRepository bookingRepository;
    private final RoomUnitMapper roomUnitMapper;

    public RoomUnitService(
            RoomRepository roomRepository,
            RoomUnitRepository roomUnitRepository,
            RoomUnitBlockRepository roomUnitBlockRepository,
            BookingRepository bookingRepository,
            RoomUnitMapper roomUnitMapper) {
        this.roomRepository = roomRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.roomUnitBlockRepository = roomUnitBlockRepository;
        this.bookingRepository = bookingRepository;
        this.roomUnitMapper = roomUnitMapper;
    }

    @Transactional(readOnly = true)
    public List<RoomUnit> list(String roomId) {
        List<RoomUnitEntity> entities = roomId != null ? roomUnitRepository.findByRoomId(roomId) : roomUnitRepository.findAll();
        return entities.stream().map(roomUnitMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public RoomUnit getById(String id) {
        return roomUnitMapper.toDto(findEntity(id));
    }

    @Transactional
    public RoomUnit create(RoomUnitInput input) {
        if (!roomRepository.existsById(input.getRoomId())) {
            throw new BadRequestException("Room not found");
        }

        RoomUnitEntity entity = new RoomUnitEntity();
        roomUnitMapper.applyInput(entity, input);
        try {
            // flush so @CreationTimestamp (populated at insert time) is on the object before mapping,
            // and so the label's unique-constraint violation (if any) surfaces here, not later.
            return roomUnitMapper.toDto(roomUnitRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This label is already in use by another room.");
        }
    }

    @Transactional
    public RoomUnit update(String id, RoomUnitUpdateInput input) {
        RoomUnitEntity entity = findEntity(id);

        if (entity.isActive() && !input.getIsActive() && hasUpcomingBooking(id)) {
            throw new ConflictException("Room " + entity.getLabel() + " has an upcoming booking assigned and can't be deactivated.");
        }

        roomUnitMapper.applyUpdate(entity, input);
        try {
            return roomUnitMapper.toDto(roomUnitRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This label is already in use by another room.");
        }
    }

    @Transactional
    public void delete(String id) {
        RoomUnitEntity entity = findEntity(id);
        if (hasUpcomingBooking(id)) {
            throw new ConflictException("Room " + entity.getLabel() + " has an upcoming booking assigned and can't be deleted.");
        }
        try {
            // Backstop for a unit that still has *past* (checked-out) bookings pointing at it -
            // hasUpcomingBooking only screens out future commitments, but Booking.roomUnitId's
            // FK is RESTRICT either way, so history is preserved rather than silently orphaned.
            roomUnitRepository.delete(entity);
            roomUnitRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("Room " + entity.getLabel() + " has bookings on record and can't be deleted.");
        }
    }

    /** A non-cancelled booking assigned to this unit whose stay hasn't ended yet - deactivating/deleting would silently orphan it. */
    private boolean hasUpcomingBooking(String roomUnitId) {
        return bookingRepository.existsByRoomUnitIdAndStatusNotAndCheckOutGreaterThan(roomUnitId, BookingStatus.CANCELLED, LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<RoomUnitBlock> listBlocks(String roomUnitId) {
        findEntity(roomUnitId);
        return roomUnitBlockRepository.findByRoomUnitId(roomUnitId).stream().map(roomUnitMapper::toDto).toList();
    }

    @Transactional
    public RoomUnitBlock createBlock(String roomUnitId, RoomUnitBlockInput input) {
        findEntity(roomUnitId);

        LocalDate fromDate = LocalDate.parse(input.getFromDate());
        LocalDate toDate = LocalDate.parse(input.getToDate());
        if (fromDate.isAfter(toDate)) {
            throw ValidationException.field("toDate", "fromDate must be on or before toDate");
        }

        RoomUnitBlockEntity entity = new RoomUnitBlockEntity();
        entity.setRoomUnitId(roomUnitId);
        entity.setFromDate(fromDate);
        entity.setToDate(toDate);
        entity.setReason(input.getReason().trim());
        return roomUnitMapper.toDto(roomUnitBlockRepository.saveAndFlush(entity));
    }

    @Transactional
    public void deleteBlock(String roomUnitId, String blockId) {
        RoomUnitBlockEntity block = roomUnitBlockRepository.findByIdAndRoomUnitId(blockId, roomUnitId)
                .orElseThrow(() -> new NotFoundException("Room unit or block not found"));
        roomUnitBlockRepository.delete(block);
    }

    private RoomUnitEntity findEntity(String id) {
        return roomUnitRepository.findById(id).orElseThrow(() -> new NotFoundException("Room unit not found"));
    }
}
