package com.sunsetbeach.bot;

import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * "Room blocks" for a housekeeper: the blocks the housekeeping function answers for
 * ({@code RoomUnitBlock.responsibleFunction = HOUSEKEEPER}) that are on now or start within
 * {@code housekeeperBlocksDays} days - the room, the dates and the reason the manager wrote. Read only: the owner
 * postponed a "room is clean" button, so there is nothing to press and nothing here writes.
 */
@Component
public class HousekeepingBlocksCommand implements BotCommandHandler {

    private final RoomUnitBlockRepository blockRepository;
    private final RoomUnitRepository unitRepository;

    public HousekeepingBlocksCommand(RoomUnitBlockRepository blockRepository, RoomUnitRepository unitRepository) {
        this.blockRepository = blockRepository;
        this.unitRepository = unitRepository;
    }

    @Override
    public String command() {
        return "blocks";
    }

    @Override
    public Key label() {
        return Key.MENU_BLOCKS;
    }

    @Override
    public boolean enabled(StaffBotSettingsEntity settings) {
        return settings.isHousekeeperBlocksEnabled();
    }

    @Override
    public boolean availableTo(UserEntity user, LocalDate today) {
        return BotAccess.hasFunction(user, "HOUSEKEEPER");
    }

    @Override
    public boolean forEveryone() {
        return false;
    }

    @Override
    public int order() {
        return 82;
    }

    @Override
    public List<BotReply> handle(BotContext ctx, String arguments) {
        int days = ctx.settings().getHousekeeperBlocksDays();
        List<RoomUnitBlockEntity> blocks = blockRepository.findByResponsibleFunctionAndFromDateLessThanEqualAndToDateGreaterThanEqualOrderByFromDateAsc(
                "HOUSEKEEPER", ctx.today().plusDays(days), ctx.today());
        Map<String, String> params = Map.of("days", String.valueOf(days));
        if (blocks.isEmpty()) {
            return List.of(BotReply.text(ctx.t(Key.BLOCKS_NONE, params)));
        }
        Map<String, String> labels = new HashMap<>();
        unitRepository.findAllById(blocks.stream().map(RoomUnitBlockEntity::getRoomUnitId).distinct().toList())
                .forEach((RoomUnitEntity u) -> labels.put(u.getId(), u.getLabel()));
        List<String> lines = new ArrayList<>();
        lines.add(ctx.t(Key.BLOCKS_TITLE, params));
        for (RoomUnitBlockEntity b : blocks) {
            String reason = b.getReason() == null || b.getReason().isBlank() ? "" : ". " + b.getReason().strip();
            lines.add(ctx.t(Key.BLOCK_LINE, Map.of(
                    "room", labels.getOrDefault(b.getRoomUnitId(), "?"), "from", b.getFromDate().toString(), "to", b.getToDate().toString(), "reason", reason)));
        }
        return List.of(BotReply.text(String.join("\n", lines)));
    }
}
