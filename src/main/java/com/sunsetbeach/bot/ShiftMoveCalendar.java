package com.sunsetbeach.bot;

import com.sunsetbeach.entity.BotConversationEntity;
import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.StaffActionEntity;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.model.ShiftChangeRequestType;
import com.sunsetbeach.notification.BotText;
import com.sunsetbeach.notification.NotificationAction.Kinds;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.service.ShiftChangeRequestService;
import com.sunsetbeach.service.ShiftChangeRequestService.Rejection;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * "Other date..." of a shift move: a month calendar of inline buttons, edited in place as it is paged, and the
 * same screen taking a typed date ({@code SHIFT_DATE} dialogue) while it is open.
 *
 * <p>Only days {@link ShiftChangeRequestService#eligibleTargets} allows (up to {@code moveMaxDaysAhead}) are
 * pressable; a day's button is the ordinary {@code SHIFT_MOVE_DATE}, so choosing it goes through exactly the
 * path a button of the first screen does ({@link ShiftRequestActions#startReason}), which checks everything
 * again against the settings and clock <em>as they are then</em> - a calendar left open past a settings change
 * or a midnight refuses instead of filing. Heading, weekdays and empty days are inert buttons (no stored action).
 * Telegram's limits (100 buttons per keyboard, 64 bytes of callback data) are not close: a month is at most
 * 3 + 7 + 42 + 1 buttons, each carrying a 14-byte id.
 */
@Component
public class ShiftMoveCalendar implements BotActionHandler, BotDialogueHandler {

    static final String STATE = "SHIFT_DATE";
    private static final String EMPTY = "·";

    private final RosterEntryRepository entryRepository;
    private final ShiftChangeRequestService requestService;
    private final BotConversations conversations;
    private final ShiftRequestActions requestActions;

    public ShiftMoveCalendar(
            RosterEntryRepository entryRepository,
            ShiftChangeRequestService requestService,
            BotConversations conversations,
            ShiftRequestActions requestActions) {
        this.entryRepository = entryRepository;
        this.requestService = requestService;
        this.conversations = conversations;
        this.requestActions = requestActions;
    }

    @Override
    public Set<String> kinds() {
        return Set.of(Kinds.SHIFT_CAL);
    }

    @Override
    public List<BotReply> handle(BotContext ctx, StaffActionEntity action) {
        RosterEntryEntity entry = entryRepository.findById(action.getSubjectId()).orElse(null);
        if (entry == null) {
            return List.of(BotReply.toastOnly(ctx.t(Key.BOT_BUTTON_EXPIRED)));
        }
        Optional<Rejection> problem = requestService.check(ctx.user(), entry, ShiftChangeRequestType.MOVE);
        if (problem.isPresent()) {
            String text = ShiftBotSupport.rejectionText(ctx, problem.get());
            return List.of(BotReply.text(text).toast(text));
        }
        List<LocalDate> eligible = requestService.eligibleTargets(ctx.user(), entry);
        if (eligible.isEmpty()) {
            return List.of(BotReply.text(ctx.t(Key.MOVE_NO_DATES)));
        }
        YearMonth month = firstMonth(ctx, eligible);
        if (action.getArg() != null) {
            try {
                month = YearMonth.parse(action.getArg());
            } catch (DateTimeParseException e) {
                // A malformed argument can only be a forged one: show the default month.
            }
        }
        // Waiting for a typed date as well; each page turn keeps the dialogue alive for another fifteen minutes.
        conversations.start(ctx.chatId(), ctx.user().getId(), STATE, entry.getId(), ShiftChangeRequestType.MOVE.getValue(), null);
        return List.of(calendar(ctx, entry, eligible, month).editCurrent());
    }

    /** The month to open on: where the buttons of the first screen stop, else the first allowed day. */
    private static YearMonth firstMonth(BotContext ctx, List<LocalDate> eligible) {
        LocalDate lastButtonDay = ctx.today().plusDays(ctx.settings().getMoveWindowDays());
        return YearMonth.from(eligible.stream().filter(d -> d.isAfter(lastButtonDay)).findFirst().orElse(eligible.get(0)));
    }

    // ---- the typed date ------------------------------------------------------------------------------------

    @Override
    public String state() {
        return STATE;
    }

    @Override
    public List<BotReply> onText(BotContext ctx, BotConversationEntity conversation, String text) {
        Optional<LocalDate> date = BotDateParser.parse(text, ctx.today());
        if (date.isEmpty()) {
            return List.of(BotReply.text(ctx.t(Key.MOVE_DATE_UNREADABLE)));
        }
        RosterEntryEntity entry = entryRepository.findById(conversation.getEntryId()).orElse(null);
        if (entry == null) {
            conversations.clear(ctx.chatId());
            return List.of(BotReply.text(ctx.t(Key.BOT_BUTTON_EXPIRED)));
        }
        Optional<Rejection> problem = requestService.check(ctx.user(), entry, ShiftChangeRequestType.MOVE);
        if (problem.isEmpty()) {
            problem = requestService.checkTarget(ctx.user(), entry, date.get());
        }
        if (problem.isPresent()) {
            // The calendar stays on screen and the dialogue open: say why in words and let them try again.
            String why = ShiftBotSupport.rejectionText(ctx, problem.get());
            return List.of(BotReply.text(ctx.t(Key.MOVE_CALENDAR_TYPED, Map.of("why", why))));
        }
        return requestActions.startReason(ctx, entry.getId(), ShiftChangeRequestType.MOVE, date.get());
    }

    // ---- the screen ----------------------------------------------------------------------------------------

    /** The month page; {@code requested} is clamped to today's month .. the month of the last allowed day. */
    static BotReply calendar(BotContext ctx, RosterEntryEntity entry, List<LocalDate> eligible, YearMonth requested) {
        NotificationLanguage language = ctx.language();
        YearMonth first = YearMonth.from(ctx.today());
        YearMonth last = YearMonth.from(ctx.today().plusDays(ctx.settings().getMoveMaxDaysAhead()));
        YearMonth month = requested.isBefore(first) ? first : requested.isAfter(last) ? last : requested;
        Set<LocalDate> allowed = new HashSet<>(eligible);

        BotReply reply = BotReply.text(ctx.t(Key.MOVE_CALENDAR, Map.of("date", BotText.date(entry.getDate(), language))));
        boolean hasPrevious = month.isAfter(first);
        boolean hasNext = month.isBefore(last);
        reply.row(
                hasPrevious ? new BotReply.Button("◀", Kinds.SHIFT_CAL, entry.getId(), month.minusMonths(1).toString()) : BotReply.Button.inert(EMPTY),
                BotReply.Button.inert(BotText.monthLong(month, language)),
                hasNext ? new BotReply.Button("▶", Kinds.SHIFT_CAL, entry.getId(), month.plusMonths(1).toString()) : BotReply.Button.inert(EMPTY));

        List<BotReply.Button> weekdays = new ArrayList<>();
        for (DayOfWeek d : DayOfWeek.values()) {
            weekdays.add(BotReply.Button.inert(BotText.weekday(d.getValue(), language)));
        }
        reply.row(weekdays);

        LocalDate day = month.atDay(1).minusDays(month.atDay(1).getDayOfWeek().getValue() - 1L);
        while (!day.isAfter(month.atEndOfMonth())) {
            List<BotReply.Button> week = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                boolean pressable = YearMonth.from(day).equals(month) && allowed.contains(day);
                week.add(pressable
                        ? new BotReply.Button(String.valueOf(day.getDayOfMonth()), Kinds.SHIFT_MOVE_DATE, entry.getId(), day.toString())
                        : BotReply.Button.inert(EMPTY));
                day = day.plusDays(1);
            }
            reply.row(week);
        }
        reply.row(BotReply.Button.of(ctx.t(Key.BTN_CANCEL), Kinds.DIALOGUE_CANCEL, null));
        return reply;
    }
}
