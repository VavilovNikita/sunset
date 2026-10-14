package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.AbstractIntegrationTest;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.model.Room;
import com.sunsetbeach.model.RoomInput;
import com.sunsetbeach.model.RoomUnit;
import com.sunsetbeach.model.RoomUnitInput;
import com.sunsetbeach.model.RoomUnitUpdateInput;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** A physical room's label is unique ignoring case and spaces: "Q1" and "q1" are one room (V143). */
@SpringBootTest
@Transactional
class RoomUnitLabelUniquenessTests extends AbstractIntegrationTest {

    @Autowired private RoomService roomService;
    @Autowired private RoomUnitService roomUnitService;
    @Autowired private RoomUnitRepository roomUnitRepository;

    private Room newType() {
        return roomService.create(new RoomInput("Label test " + UUID.randomUUID(), "A room used only by RoomUnitLabelUniquenessTests", 2, new BigDecimal("1500")));
    }

    @Test
    void aSecondRoomWithTheLabelInAnotherCase_isRefused() {
        Room type = newType();
        String label = "Q" + UUID.randomUUID().toString().substring(0, 8);
        roomUnitService.create(new RoomUnitInput(type.getId(), label));

        assertThatThrownBy(() -> roomUnitService.create(new RoomUnitInput(type.getId(), label.toLowerCase())))
                .isInstanceOf(ConflictException.class);
        assertThat(roomUnitRepository.existsByNormalizedLabel("  " + label.toLowerCase() + " ", null)).isTrue();
    }

    @Test
    void innerSpacesDoNotMakeALabelDifferent() {
        Room type = newType();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        roomUnitService.create(new RoomUnitInput(type.getId(), "Villa 1 " + suffix));

        assertThatThrownBy(() -> roomUnitService.create(new RoomUnitInput(type.getId(), "villa   1 " + suffix)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void renamingOntoAnotherLabelIsRefused_butChangingTheCaseOfOwnLabelIsFine() {
        Room type = newType();
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        RoomUnit first = roomUnitService.create(new RoomUnitInput(type.getId(), "A" + suffix));
        RoomUnit second = roomUnitService.create(new RoomUnitInput(type.getId(), "B" + suffix));

        assertThatThrownBy(() -> roomUnitService.update(second.getId(), new RoomUnitUpdateInput(("a" + suffix), true)))
                .isInstanceOf(ConflictException.class);
        RoomUnit renamed = roomUnitService.update(first.getId(), new RoomUnitUpdateInput(("a" + suffix), true));
        assertThat(renamed.getLabel()).isEqualTo("a" + suffix);
    }
}
