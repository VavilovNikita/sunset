package com.sunsetbeach.service;

import com.sunsetbeach.entity.RosterChangeMarkEntity;
import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.NotificationType;
import com.sunsetbeach.notification.BotText;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.repository.BotConversationRepository;
import com.sunsetbeach.repository.RosterChangeMarkRepository;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.StaffActionRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * The staff bot's periodic work, run from {@link NotificationScanService#runOnce} (so it is the project's one
 * once-a-minute {@code @Scheduled} sweep, not a new scheduler). Nothing here keeps state of its own: every
 * condition is re-derived from the data on each pass and a message is raised under a dedup key the database
 * makes unique, so a backend that was down simply catches up and a repeated pass raises nothing twice.
 *
 * <ul>
 *   <li>shift requests whose shift has begun undecided expire; deciders are reminded of long-waiting ones;
 *   <li>the evening-before reminder of tomorrow's shift;
 *   <li>"your schedule changed", once per employee and day, after a manager's changes stop for the configured
 *       delay - the message carries the final state, and nothing is sent when the day ended up as it was;
 *   <li>housekeeping of expired button rows and dialogues.
 * </ul>
 */
@Service
public class StaffBotScanService {

    private static final Logger log = LoggerFactory.getLogger(StaffBotScanService.class);

    private final StaffBotSettingsService settingsService;
    private final ShiftChangeRequestService requestService;
    private final NotificationService notificationService;
    private final RosterChangeMarkRepository markRepository;
    private final RosterEntryRepository entryRepository;
    private final ShiftCodeRepository shiftCodeRepository;
    private final UserRepository userRepository;
    private final StaffActionRepository actionRepository;
    private final BotConversationRepository conversationRepository;
    private final Clock clock;
    private final TransactionTemplate tx;

    public StaffBotScanService(
            StaffBotSettingsService settingsService,
            ShiftChangeRequestService requestService,
            NotificationService notificationService,
            RosterChangeMarkRepository markRepository,
            RosterEntryRepository entryRepository,
            ShiftCodeRepository shiftCodeRepository,
            UserRepository userRepository,
            StaffActionRepository actionRepository,
            BotConversationRepository conversationRepository,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.settingsService = settingsService;
        this.requestService = requestService;
        this.notificationService = notificationService;
        this.markRepository = markRepository;
        this.entryRepository = entryRepository;
        this.shiftCodeRepository = shiftCodeRepository;
        this.userRepository = userRepository;
        this.actionRepository = actionRepository;
        this.conversationRepository = conversationRepository;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    /** One pass; the parts are independent, one failing doesn't skip the others. */
    public void runOnce() {
        StaffBotSettingsEntity settings = settingsService.getEntity();
        run("shift request expiry", () -> {
            if (settings.isRequestsEnabled()) {
                requestService.expireStarted();
            }
        });
        run("shift request reminders", requestService::remindPending);
        run("evening reminders", () -> sendEveReminders(settings));
        run("roster change messages", () -> announceRosterChanges(settings));
        run("bot housekeeping", this::cleanUp);
    }

    private void run(String what, Runnable part) {
        try {
            part.run();
        } catch (RuntimeException e) {
            log.error("Staff bot sweep ({}) failed", what, e);
        }
    }

    // ---- evening before ---------------------------------------------------------------------------------

    /** From {@code eveReminderTime} on, tomorrow's shift goes to everyone who wants the reminder; once per person and shift day. */
    int sendEveReminders(StaffBotSettingsEntity settings) {
        if (!settings.isEveReminderEnabled() || LocalDateTime.now(clock).toLocalTime().isBefore(settings.getEveReminderTime())) {
            return 0;
        }
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        List<RosterEntryEntity> entries = entryRepository.findByDateBetween(tomorrow, tomorrow);
        if (entries.isEmpty()) {
            return 0;
        }
        Map<String, ShiftCodeEntity> codes = new HashMap<>();
        shiftCodeRepository.findAllById(entries.stream().map(RosterEntryEntity::getShiftCodeId).distinct().toList()).forEach(c -> codes.put(c.getId(), c));
        Map<String, UserEntity> users = new HashMap<>();
        userRepository.findAllById(entries.stream().map(RosterEntryEntity::getEmployeeUserId).distinct().toList()).forEach(u -> users.put(u.getId(), u));
        int raised = 0;
        for (RosterEntryEntity entry : entries) {
            UserEntity user = users.get(entry.getEmployeeUserId());
            ShiftCodeEntity code = codes.get(entry.getShiftCodeId());
            if (user == null || !user.isActive() || code == null || !code.isCountsAsWorked()) {
                continue;
            }
            boolean wanted = user.getEveReminderEnabled() != null ? user.getEveReminderEnabled() : settings.isEveReminderDefaultOn();
            if (!wanted) {
                continue;
            }
            Map<String, String> params = new HashMap<>();
            BotText.perLanguage(params, "date", l -> BotText.date(tomorrow, l));
            params.put("shift", BotText.shift(code.getCode(), ShiftLabels.times(code)));
            raised += notificationService.raise(
                    NotificationType.SHIFT_EVE_REMINDER, Key.SHIFT_EVE_REMINDER, List.of(user.getId()), params, "EVE:" + user.getId() + ":" + tomorrow);
        }
        return raised;
    }

    // ---- the roster changed -------------------------------------------------------------------------------

    /** Announces every mark whose last change is at least {@code rosterChangeDelayMinutes} old. Returns how many messages were raised. */
    int announceRosterChanges(StaffBotSettingsEntity settings) {
        LocalDateTime nowUtc = TimestampFormat.nowUtc(clock);
        LocalDate today = LocalDate.now(clock);
        int raised = 0;
        for (RosterChangeMarkEntity mark : markRepository.findByLastChangedAtLessThanEqual(nowUtc.minusMinutes(settings.getRosterChangeDelayMinutes()))) {
            try {
                raised += announce(mark, settings, today);
            } catch (RuntimeException e) {
                log.error("Announcing the roster change {} failed", mark.getId(), e);
            }
        }
        return raised;
    }

    private int announce(RosterChangeMarkEntity mark, StaffBotSettingsEntity settings, LocalDate today) {
        int raised = 0;
        UserEntity user = userRepository.findById(mark.getEmployeeUserId()).orElse(null);
        RosterEntryEntity now = entryRepository.findByEmployeeUserIdAndDate(mark.getEmployeeUserId(), mark.getShiftDate()).orElse(null);
        String afterCodeId = now == null ? null : now.getShiftCodeId();
        boolean unchanged = Objects.equals(mark.getBeforeShiftCodeId(), afterCodeId);
        boolean relevant = settings.isRosterChangeEnabled() && user != null && user.isActive() && !unchanged
                && !mark.getShiftDate().isBefore(today) && !mark.getShiftDate().isAfter(today.plusDays(settings.getRosterChangeHorizonDays()));
        if (relevant) {
            Map<String, String> params = new HashMap<>();
            BotText.perLanguage(params, "date", l -> BotText.date(mark.getShiftDate(), l));
            BotText.perLanguage(params, "before", l -> describeShift(mark.getBeforeShiftCodeId(), l));
            BotText.perLanguage(params, "after", l -> describeShift(afterCodeId, l));
            raised = notificationService.raise(
                    NotificationType.ROSTER_CHANGED, Key.ROSTER_CHANGED, List.of(user.getId()), params,
                    "ROSTER_CHANGED:" + user.getId() + ":" + mark.getShiftDate() + ":" + mark.getId());
        }
        // Only if it was not touched again meanwhile: a change that landed during this pass keeps its mark for the next.
        tx.executeWithoutResult(s -> markRepository.deleteIfUnchanged(mark.getId(), mark.getLastChangedAt()));
        return raised;
    }

    private String describeShift(String shiftCodeId, com.sunsetbeach.model.NotificationLanguage language) {
        if (shiftCodeId == null) {
            return NotificationMessages.render(Key.FRAG_DAY_OFF, language, Map.of());
        }
        return shiftCodeRepository.findById(shiftCodeId).map(c -> BotText.shift(c.getCode(), ShiftLabels.times(c)))
                .orElse(NotificationMessages.render(Key.FRAG_DAY_OFF, language, Map.of()));
    }

    // ---- housekeeping ---------------------------------------------------------------------------------------

    private void cleanUp() {
        LocalDateTime nowUtc = TimestampFormat.nowUtc(clock);
        tx.executeWithoutResult(s -> {
            actionRepository.deleteExpired(nowUtc);
            conversationRepository.deleteExpired(nowUtc);
        });
    }
}
