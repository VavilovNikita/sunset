package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomInput;
import com.sunsetbeach.repository.RoomRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

/**
 * A room type's name is unique ignoring case and spaces at either end. Two "Deluxe" rows in the
 * reference list can't be told apart by a receptionist, so a second one is refused with a readable
 * 409 - by the service in the ordinary case, and by the V138 unique index when two writes race.
 */
@SpringBootTest
@Transactional
class RoomNameUniquenessTests extends AbstractIntegrationTest {

    @Autowired private RoomService roomService;
    @Autowired private RoomRepository roomRepository;

    private static RoomInput input(String name) {
        return new RoomInput(name, "A room used only by RoomNameUniquenessTests", 2, new BigDecimal("1500"));
    }

    private static String unique(String base) {
        return base + " " + UUID.randomUUID();
    }

    @Test
    void aSecondTypeWithTheSameName_ignoringCaseAndEdgeSpaces_isRefused() {
        String name = unique("Deluxe");
        roomService.create(input(name));

        assertThatThrownBy(() -> roomService.create(input(name.toUpperCase()))).isInstanceOf(ConflictException.class);
        // The model strips edge spaces itself; the repository rule must agree with that anyway.
        assertThat(roomRepository.existsByNormalizedName("  " + name.toLowerCase() + "  ", null)).isTrue();
    }

    @Test
    void renamingATypeOntoAnotherOne_isRefused_butKeepingItsOwnNameIsFine() {
        Room first = roomService.create(input(unique("Garden")));
        Room second = roomService.create(input(unique("Seaview")));

        assertThatThrownBy(() -> roomService.update(second.getId(), input(first.getName().toLowerCase()))).isInstanceOf(ConflictException.class);

        // Editing the price of a type under its own name, in another case, must not collide with itself.
        Room edited = roomService.update(first.getId(), new RoomInput(first.getName().toUpperCase(), "A room used only by RoomNameUniquenessTests", 3, new BigDecimal("1800")));
        assertThat(edited.getCapacity()).isEqualTo(3);
    }

    @Test
    void theDatabaseIndexHoldsWhenTheServiceCheckIsBypassed() {
        String name = unique("Bungalow");
        roomService.create(input(name));

        RoomEntity duplicate = new RoomEntity();
        duplicate.setName(" " + name.toUpperCase() + " ");
        duplicate.setDescription("A room used only by RoomNameUniquenessTests");
        duplicate.setCapacity(2);
        duplicate.setBasePrice(new BigDecimal("1500.00"));

        assertThatThrownBy(() -> roomRepository.saveAndFlush(duplicate)).isInstanceOf(DataIntegrityViolationException.class);
    }
}
