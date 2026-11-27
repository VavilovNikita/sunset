package com.sunsetbeach.service;

import com.sunsetbeach.entity.ManualUpdateEntryEntity;
import com.sunsetbeach.entity.NotificationSettingsEntity;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.NotificationType;
import com.sunsetbeach.notification.ManualUpdateFeed;
import com.sunsetbeach.notification.ManualUpdateFeed.Note;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.repository.ManualUpdateEntryRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Tells the directors that the staff manual's "What's new" has a new note ({@link ManualUpdateFeed}) - the
 * project's usual {@code @Scheduled} fixed-delay sweep, through {@link NotificationService#raise}, so quiet hours
 * hold the delivery like any other message.
 *
 * <ul>
 *   <li><b>No history.</b> A note is told about only if it is dated yesterday (hotel day) or later when the server
 *       first sees it: the first run, a long outage or an old note published late send nothing.</li>
 *   <li><b>Once per note</b>, by its id - correcting a published note sends nothing.</li>
 *   <li><b>One message a hotel day at most.</b> Notes that are new at the same time share one message. A note that
 *       appears when the day already had its message is held and goes with the next day's. The day is the one the
 *       message is <em>delivered</em> on: raised at 23:00 inside quiet hours, it is tomorrow's.</li>
 *   <li><b>Switched off, nothing is saved up:</b> what appears meanwhile is marked as never to be sent.</li>
 * </ul>
 *
 * <p>The record ({@code ManualUpdateEntry}) is written in the same transaction as the notification, under a
 * transaction lock, so two passes cannot both find the day free.
 */
@Service
public class ManualUpdateService {

    private static final Logger log = LoggerFactory.getLogger(ManualUpdateService.class);

    /** How old a note may be when first seen. */
    private static final int FRESH_DAYS = 1;
    /** A held note waits one day for its turn, so by then it is this much older. */
    private static final int HELD_DAYS = FRESH_DAYS + 1;
    /** Telegram refuses a message over 4096 characters. */
    private static final int MAX_NOTES_IN_A_MESSAGE = 5;
    private static final int MAX_SUMMARY = 500;
    private static final int MAX_TITLE = 150;

    private final ManualUpdateFeed feed;
    private final ManualUpdateEntryRepository repository;
    private final NotificationSettingsService settingsService;
    private final NotificationService notificationService;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final String baseUrl;
    private final boolean sweepEnabled;

    public ManualUpdateService(
            ManualUpdateFeed feed,
            ManualUpdateEntryRepository repository,
            NotificationSettingsService settingsService,
            NotificationService notificationService,
            Clock clock,
            PlatformTransactionManager transactionManager,
            @Value("${app.manual.base-url}") String baseUrl,
            @Value("${app.manual.sweep-enabled:true}") boolean sweepEnabled) {
        this.feed = feed;
        this.repository = repository;
        this.settingsService = settingsService;
        this.notificationService = notificationService;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
        this.baseUrl = baseUrl.trim().replaceAll("/+$", "");
        this.sweepEnabled = sweepEnabled;
    }

    @Scheduled(fixedDelayString = "${app.manual.poll-interval-ms:600000}", initialDelayString = "${app.manual.initial-delay-ms:60000}")
    public void sweep() {
        if (!sweepEnabled) {
            return;
        }
        try {
            runOnce();
        } catch (RuntimeException e) {
            log.error("Telling the directors about a manual update failed", e);
        }
    }

    /** One pass over the feed as it is now. */
    public void runOnce() {
        feed.read().ifPresent(notes -> tx.executeWithoutResult(status -> announce(notes)));
    }

    /**
     * For the moment the switch is turned back on: what was published while it was off, and not looked at since, is
     * marked as never to be sent. Never fails the settings save it accompanies.
     */
    public void skipPublished() {
        try {
            feed.read().ifPresent(notes -> tx.executeWithoutResult(status -> {
                repository.lockAnnouncements();
                Map<String, ManualUpdateEntryEntity> known = known(notes);
                pending(notes, known, LocalDate.now(clock)).forEach(note -> remember(known, note, ManualUpdateEntryEntity.SKIPPED, null));
            }));
        } catch (RuntimeException e) {
            log.error("Marking the already published manual updates as skipped failed", e);
        }
    }

    private void announce(List<Note> notes) {
        repository.lockAnnouncements();
        NotificationSettingsEntity settings = settingsService.getEntity();
        Map<String, ManualUpdateEntryEntity> known = known(notes);
        List<Note> pending = pending(notes, known, LocalDate.now(clock));
        if (pending.isEmpty()) {
            return;
        }
        if (!settings.isManualUpdatesEnabled()) {
            pending.forEach(note -> remember(known, note, ManualUpdateEntryEntity.SKIPPED, null));
            return;
        }
        List<String> directors = Arrays.asList(settings.getDirectorUserIds());
        if (directors.isEmpty()) {
            return; // nobody to tell: left as it is, a director chosen today still hears about today's note
        }
        LocalDateTime dueUtc = notificationService.deliverAfter(settings, TimestampFormat.nowUtc(clock));
        LocalDate dueDay = dueUtc.atOffset(ZoneOffset.UTC).atZoneSameInstant(clock.getZone()).toLocalDate();
        if (repository.existsBySentOn(dueDay)) {
            pending.stream().filter(note -> !known.containsKey(note.id())).forEach(note -> remember(known, note, ManualUpdateEntryEntity.HELD, null));
            return;
        }
        List<Note> shown = pending.stream().limit(MAX_NOTES_IN_A_MESSAGE).toList();
        int raised = notificationService.raise(
                NotificationType.MANUAL_UPDATED,
                NotificationMessages.Key.MANUAL_UPDATED,
                directors,
                Map.of("notes.EN", text(shown, "en"), "notes.TH", text(shown, "th"), "notes.RU", text(shown, "en")),
                "MANUAL_UPDATE:" + shown.get(0).id());
        if (raised == 0) {
            return; // no active director among those chosen
        }
        pending.forEach(note -> remember(known, note, ManualUpdateEntryEntity.SENT, dueDay));
    }

    private Map<String, ManualUpdateEntryEntity> known(List<Note> notes) {
        return repository.findAllById(notes.stream().map(Note::id).toList()).stream()
                .collect(Collectors.toMap(ManualUpdateEntryEntity::getEntryId, Function.identity()));
    }

    /** The notes still owed to someone, newest first: unseen and recent, or held for a free day. */
    private static List<Note> pending(List<Note> notes, Map<String, ManualUpdateEntryEntity> known, LocalDate today) {
        List<Note> pending = new ArrayList<>();
        for (Note note : notes) {
            ManualUpdateEntryEntity row = known.get(note.id());
            boolean fresh = row == null && !note.date().isBefore(today.minusDays(FRESH_DAYS));
            boolean held = row != null && ManualUpdateEntryEntity.HELD.equals(row.getState()) && !note.date().isBefore(today.minusDays(HELD_DAYS));
            if (fresh || held) {
                pending.add(note);
            }
        }
        pending.sort(Comparator.comparing(Note::date).reversed().thenComparing(Note::id));
        return pending;
    }

    private void remember(Map<String, ManualUpdateEntryEntity> known, Note note, String state, LocalDate sentOn) {
        ManualUpdateEntryEntity row = known.get(note.id());
        if (row == null) {
            row = new ManualUpdateEntryEntity();
            row.setEntryId(note.id());
        }
        row.setState(state);
        row.setSentOn(sentOn);
        repository.save(row);
    }

    /** "{title}. {summary}" and the page's address for each note, in the wanted language where the note has it. */
    private String text(List<Note> notes, String wanted) {
        return notes.stream().map(note -> {
            String language = note.page().containsKey(wanted) ? wanted
                    : note.page().containsKey("en") ? "en" : note.page().keySet().iterator().next();
            String title = cut(note.title().get(language), MAX_TITLE);
            String summary = cut(note.summary().getOrDefault(language, ""), MAX_SUMMARY);
            String heading = summary.isEmpty() ? title : (title.matches(".*[.!?…]") ? title : title + ".") + " " + summary;
            return heading + "\n" + baseUrl + "/" + note.page().get(language);
        }).collect(Collectors.joining("\n\n"));
    }

    private static String cut(String text, int max) {
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }
}
