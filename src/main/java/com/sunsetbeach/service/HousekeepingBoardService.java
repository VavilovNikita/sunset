package com.sunsetbeach.service;

import com.sunsetbeach.model.HousekeepingRoom;
import com.sunsetbeach.model.OccupancyStatus;
import com.sunsetbeach.model.PropertyMapCurrentBooking;
import com.sunsetbeach.model.PropertyMapUnit;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /housekeeping/rooms}: the property map's answer to "is someone in this room", cut down to
 * what cleaning needs. Who is where is {@link PropertyMapService}'s (and so {@link OverstayRule}'s); a tile
 * stands for one guest, so "leaving today" and "overdue" are asked of everybody {@link OverstayRule} puts in
 * the room - the one who has not left it counts, not only the one moved in. Every field of the result is built
 * by name, so nothing about a guest or about money can reach it by being added to the map.
 */
@Service
public class HousekeepingBoardService {

    private final PropertyMapService propertyMapService;
    private final OverstayRule overstayRule;
    private final Clock clock;

    public HousekeepingBoardService(PropertyMapService propertyMapService, OverstayRule overstayRule, Clock clock) {
        this.propertyMapService = propertyMapService;
        this.overstayRule = overstayRule;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<HousekeepingRoom> rooms() {
        String today = LocalDate.now(clock).toString();
        Map<String, List<OverstayRule.InHouseGuest>> inHouse = overstayRule.inHouseByRoomUnit();
        return propertyMapService.get().getUnits().stream()
                .map(unit -> toRoom(unit, inHouse.getOrDefault(unit.getRoomUnitId(), List.of()), today))
                .toList();
    }

    private static HousekeepingRoom toRoom(PropertyMapUnit unit, List<OverstayRule.InHouseGuest> guests, String today) {
        PropertyMapCurrentBooking booking = unit.getCurrentBooking();
        boolean occupied = booking != null && booking.getOccupancyStatus() == OccupancyStatus.CHECKED_IN;
        // An arrival is the map's current booking in an empty room, and the one waiting for it in an occupied one.
        boolean arriving = (booking != null && booking.getOccupancyStatus() == OccupancyStatus.EXPECTED) || unit.getWaitingArrival() != null;
        return new HousekeepingRoom(
                unit.getRoomUnitId(),
                unit.getRoomId(),
                unit.getRoomName(),
                unit.getUnitLabel(),
                unit.getIsActive(),
                unit.getHousekeepingStatus(),
                occupied,
                arriving,
                guests.stream().anyMatch(OverstayRule.InHouseGuest::dueOutToday),
                guests.stream().mapToInt(OverstayRule.InHouseGuest::overdueDays).max().orElse(0),
                unit.getActiveBlock() != null);
    }
}
