package com.sunsetbeach.bot;

import com.sunsetbeach.entity.MaintenanceTaskEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.StaffActionEntity;
import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.MaintenanceTaskStatus;
import com.sunsetbeach.notification.NotificationAction.Kinds;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.repository.MaintenanceTaskRepository;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.service.AuditLogService;
import com.sunsetbeach.service.MaintenanceTaskService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * "Tasks" for an engineer (and, as the fallback, a manager - the rule of {@code PATCH /maintenance-tasks/{id}/status}):
 * the open and started maintenance tasks, each its own message with Start and Done.
 *
 * <p><b>A press goes through {@link MaintenanceTaskService#updateStatus}</b> - the same forward-only transition, the same
 * shortening of the room's block when a task is closed ({@code liftBlock}), the same audit entries - bound to the person
 * who pressed ({@link AuditLogService#runAs}, "via Telegram"). The bot has no second way to close a task. The web closes a
 * task with nothing more than the status, so nothing the web needs is out of the bot's reach (photos are filed, not
 * required, and are not taken here).
 *
 * <p>A button is days old at worst: the person's function and the task's status are asked again at every press, and a
 * task that was moved meanwhile (another engineer, the admin screen, a second press) answers with its current status and
 * changes nothing. {@code updateStatus} locks the task row, so two simultaneous "Done" close it once.
 */
@Component
public class EngineerTasksCommand implements BotCommandHandler, BotActionHandler {

    private static final int SHOWN = 10;
    private static final int DESCRIPTION_LIMIT = 300;

    private final MaintenanceTaskRepository taskRepository;
    private final MaintenanceTaskService taskService;
    private final RoomUnitRepository unitRepository;
    private final RoomUnitBlockRepository blockRepository;
    private final Clock clock;

    public EngineerTasksCommand(
            MaintenanceTaskRepository taskRepository,
            MaintenanceTaskService taskService,
            RoomUnitRepository unitRepository,
            RoomUnitBlockRepository blockRepository,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.taskService = taskService;
        this.unitRepository = unitRepository;
        this.blockRepository = blockRepository;
        this.clock = clock;
    }

    @Override
    public String command() {
        return "tasks";
    }

    @Override
    public Key label() {
        return Key.MENU_TASKS;
    }

    @Override
    public boolean enabled(StaffBotSettingsEntity settings) {
        return settings.isEngineerTasksEnabled();
    }

    @Override
    public boolean availableTo(UserEntity user, LocalDate today) {
        return BotAccess.engineerOrManagerPlus(user);
    }

    @Override
    public boolean forEveryone() {
        return false;
    }

    @Override
    public int order() {
        return 80;
    }

    @Override
    public List<BotReply> handle(BotContext ctx, String arguments) {
        List<MaintenanceTaskEntity> open = taskRepository.findByStatusIn(List.of(MaintenanceTaskStatus.OPEN, MaintenanceTaskStatus.IN_PROGRESS)).stream()
                .sorted(Comparator.comparing(MaintenanceTaskEntity::getCreatedAt))
                .toList();
        if (open.isEmpty()) {
            return List.of(BotReply.text(ctx.t(Key.TASKS_NONE)));
        }
        List<BotReply> replies = new ArrayList<>();
        replies.add(BotReply.text(ctx.t(Key.TASKS_TITLE)));
        for (MaintenanceTaskEntity task : open.stream().limit(SHOWN).toList()) {
            replies.add(taskMessage(ctx, task));
        }
        if (open.size() > SHOWN) {
            replies.add(BotReply.text(ctx.t(Key.TASKS_MORE, Map.of("count", String.valueOf(open.size() - SHOWN)))));
        }
        return replies;
    }

    @Override
    public Set<String> kinds() {
        return Set.of(Kinds.TASK_START, Kinds.TASK_DONE);
    }

    @Override
    public List<BotReply> handle(BotContext ctx, StaffActionEntity action) {
        // Asked again at the press: the switch, the person's function (a button can be days old) and the task.
        if (!ctx.settings().isEngineerTasksEnabled() || !BotAccess.engineerOrManagerPlus(ctx.user())) {
            throw new ForbiddenException("Not allowed to progress maintenance tasks");
        }
        MaintenanceTaskStatus target = Kinds.TASK_START.equals(action.getKind()) ? MaintenanceTaskStatus.IN_PROGRESS : MaintenanceTaskStatus.DONE;
        UserEntity user = ctx.user();
        AuditLogService.Actor actor = new AuditLogService.Actor(user.getId(), user.getEmail() == null ? user.getName() : user.getEmail(), user.getRole(), "Telegram");
        boolean changed;
        try {
            AuditLogService.runAs(actor, () -> taskService.updateStatus(action.getSubjectId(), target));
            changed = true;
        } catch (NotFoundException e) {
            return List.of(BotReply.toastOnly(ctx.t(Key.BOT_BUTTON_EXPIRED)));
        } catch (ConflictException e) {
            changed = false; // not a forward move: someone (or this very button, twice) already got it there
        }
        MaintenanceTaskEntity task = taskRepository.findById(action.getSubjectId()).orElse(null);
        if (task == null) {
            return List.of(BotReply.toastOnly(ctx.t(Key.BOT_BUTTON_EXPIRED)));
        }
        String toast = changed
                ? ctx.t(target == MaintenanceTaskStatus.DONE ? Key.TOAST_TASK_DONE : Key.TOAST_TASK_STARTED)
                : ctx.t(Key.TOAST_TASK_ALREADY, Map.of("status", ctx.t(stateKey(task.getStatus()))));
        return List.of(taskMessage(ctx, task).editCurrent().toast(toast));
    }

    /** The task as one message; the buttons are only the moves that are still possible. */
    private BotReply taskMessage(BotContext ctx, MaintenanceTaskEntity task) {
        RoomUnitEntity unit = unitRepository.findById(task.getRoomUnitId()).orElse(null);
        String room = unit == null ? "?" : unit.getLabel();
        String description = task.getDescription().length() > DESCRIPTION_LIMIT ? task.getDescription().substring(0, DESCRIPTION_LIMIT) + "..." : task.getDescription();
        if (task.getStatus() == MaintenanceTaskStatus.DONE) {
            return BotReply.text(ctx.t(Key.TASK_CLOSED_LINE, Map.of("room", room, "description", description)));
        }
        long days = ChronoUnit.DAYS.between(TimestampFormat.inHotelZone(task.getCreatedAt(), clock).toLocalDate(), ctx.today());
        RoomUnitBlockEntity block = task.getBlockId() == null ? null : blockRepository.findById(task.getBlockId()).orElse(null);
        String blockText = block == null ? ctx.t(Key.TASK_NO_BLOCK) : ctx.t(Key.TASK_BLOCK_UNTIL, Map.of("to", block.getToDate().toString()));
        BotReply reply = BotReply.text(ctx.t(Key.TASK_LINE, Map.of(
                "room", room, "description", description, "days", String.valueOf(Math.max(0, days)),
                "state", ctx.t(stateKey(task.getStatus())), "block", blockText)));
        List<BotReply.Button> buttons = new ArrayList<>();
        if (task.getStatus() == MaintenanceTaskStatus.OPEN) {
            buttons.add(BotReply.Button.of(ctx.t(Key.BTN_TASK_START), Kinds.TASK_START, task.getId()));
        }
        buttons.add(BotReply.Button.of(ctx.t(Key.BTN_TASK_DONE), Kinds.TASK_DONE, task.getId()));
        return reply.row(buttons);
    }

    private static Key stateKey(MaintenanceTaskStatus status) {
        return switch (status) {
            case OPEN -> Key.TASK_STATE_OPEN;
            case IN_PROGRESS -> Key.TASK_STATE_IN_PROGRESS;
            case DONE -> Key.TASK_STATE_DONE;
        };
    }
}
