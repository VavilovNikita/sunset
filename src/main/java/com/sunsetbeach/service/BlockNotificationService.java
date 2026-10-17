package com.sunsetbeach.service;

import com.sunsetbeach.entity.NotificationSettingsEntity;
import com.sunsetbeach.entity.RoomUnitBlockEntity;
import com.sunsetbeach.entity.RoomUnitEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.MaintenanceTaskStatus;
import com.sunsetbeach.model.NotificationType;
import com.sunsetbeach.model.TodayShiftState;
import com.sunsetbeach.model.TodayShiftStatus;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.repository.RoomUnitBlockRepository;
import com.sunsetbeach.repository.RoomUnitRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The room-block notifications: when a block is created, on the morning of its last day, and every
 * morning after it ended while its maintenance task is still not done.
 *
 * <p><b>Who gets one.</b> Everyone who is on shift <em>right now</em> and holds the block's
 * responsible function. "On shift" is {@code ON_SHIFT} from {@link AttendanceService#getTodayShiftBoard}
 * - the same derivation as {@code GET /attendance/today}, so this class has no idea of its own who is
 * at work. If nobody qualifies - or the block has no responsible function (it predates V148) - the
 * director recipients get it instead, so a block never goes unannounced.
 *
 * <p>The morning reminders are idempotent per block and day ({@code dedupKey}), so the minute scanner
 * can run them all morning, and a backend that was down at 09:00 catches up when it starts - once.
 */
@Service
public class BlockNotificationService {

    private static final Logger log = LoggerFactory.getLogger(BlockNotificationService.class);

    private static final String PATH = "/admin/maintenance";

    private final RoomUnitBlockRepository blockRepository;
    private final RoomUnitRepository roomUnitRepository;
    private final UserRepository userRepository;
    private final AttendanceService attendanceService;
    private final NotificationSettingsService settingsService;
    private final NotificationService notificationService;
    private final Clock clock;
    private final TransactionTemplate tx;

    public BlockNotificationService(
            RoomUnitBlockRepository blockRepository,
            RoomUnitRepository roomUnitRepository,
            UserRepository userRepository,
            AttendanceService attendanceService,
            NotificationSettingsService settingsService,
            NotificationService notificationService,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.blockRepository = blockRepository;
        this.roomUnitRepository = roomUnitRepository;
        this.userRepository = userRepository;
        this.attendanceService = attendanceService;
        this.settingsService = settingsService;
        this.notificationService = notificationService;
        this.clock = clock;
        // REQUIRES_NEW: onBlockCreated runs from the afterCommit callback of the block's own transaction, where
        // joining that (already committed) transaction would silently leave the writes uncommitted.
        this.tx = new TransactionTemplate(transactionManager);
        this.tx.setPropagationBehavior(org.springframework.transaction.TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    /** Runs after the block's transaction committed (see {@link NotificationEvents}); its own transaction. */
    public void onBlockCreated(String blockId) {
        tx.executeWithoutResult(status -> {
            NotificationSettingsEntity settings = settingsService.getEntity();
            if (!settings.isBlockCreatedEnabled()) {
                return;
            }
            RoomUnitBlockEntity block = blockRepository.findById(blockId).orElse(null);
            if (block == null) {
                return;
            }
            raise(NotificationType.BLOCK_CREATED, NotificationMessages.Key.BLOCK_CREATED, block, settings, "BLOCK_CREATED:" + blockId);
        });
    }

    /**
     * The morning pass: nothing before {@code morningTime} (hotel time). Safe to call every minute.
     */
    public void sendMorningReminders() {
        NotificationSettingsEntity settings = settingsService.getEntity();
        LocalTime nowTime = LocalTime.now(clock);
        if (nowTime.isBefore(settings.getMorningTime())) {
            return;
        }
        LocalDate today = LocalDate.now(clock);
        if (settings.isBlockLastDayEnabled()) {
            for (RoomUnitBlockEntity block : blockRepository.findByToDate(today)) {
                guarded(block, () -> tx.executeWithoutResult(status ->
                        raise(NotificationType.BLOCK_LAST_DAY, NotificationMessages.Key.BLOCK_LAST_DAY, block, settings, "BLOCK_LAST_DAY:" + block.getId() + ":" + today)));
            }
        }
        if (settings.isBlockExpiredEnabled()) {
            for (RoomUnitBlockEntity block : blockRepository.findEndedWithOpenTask(today, MaintenanceTaskStatus.DONE)) {
                guarded(block, () -> tx.executeWithoutResult(status ->
                        raise(NotificationType.BLOCK_EXPIRED_OPEN, NotificationMessages.Key.BLOCK_EXPIRED_OPEN, block, settings, "BLOCK_EXPIRED_OPEN:" + block.getId() + ":" + today)));
            }
        }
    }

    private void guarded(RoomUnitBlockEntity block, Runnable work) {
        try {
            work.run();
        } catch (RuntimeException e) {
            log.error("Block reminder for {} failed", block.getId(), e);
        }
    }

    private void raise(
            NotificationType type, NotificationMessages.Key key, RoomUnitBlockEntity block, NotificationSettingsEntity settings, String dedupKey) {
        RoomUnitEntity unit = roomUnitRepository.findById(block.getRoomUnitId()).orElse(null);
        if (unit == null) {
            return;
        }
        List<String> recipients = recipientsFor(block.getResponsibleFunction(), settings);
        notificationService.raise(
                type,
                key,
                recipients,
                Map.of(
                        "room", unit.getLabel(),
                        "from", block.getFromDate().toString(),
                        "to", block.getToDate().toString(),
                        "function", block.getResponsibleFunction() == null ? "ENGINEER" : block.getResponsibleFunction(),
                        NotificationService.PATH, PATH),
                dedupKey);
    }

    /**
     * People on shift now who hold {@code function}; if there are none (or {@code function} is null,
     * a block from before the field existed) the director recipients.
     */
    List<String> recipientsFor(String function, NotificationSettingsEntity settings) {
        List<String> directors = new ArrayList<>(Arrays.asList(settings.getDirectorUserIds()));
        if (function == null) {
            return directors;
        }
        Set<String> onShift = attendanceService.getTodayShiftBoard().stream()
                .filter(s -> s.getState() == TodayShiftState.ON_SHIFT)
                .map(TodayShiftStatus::getEmployeeUserId)
                .collect(Collectors.toSet());
        if (onShift.isEmpty()) {
            return directors;
        }
        List<String> qualified = userRepository.findAllById(onShift).stream()
                .filter(UserEntity::isActive)
                .filter(u -> Arrays.asList(u.getJobFunctions()).contains(function))
                .map(UserEntity::getId)
                .toList();
        return qualified.isEmpty() ? directors : qualified;
    }
}
