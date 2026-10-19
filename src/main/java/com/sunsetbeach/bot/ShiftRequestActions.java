package com.sunsetbeach.bot;

import com.sunsetbeach.entity.BotConversationEntity;
import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.ShiftChangeRequestEntity;
import com.sunsetbeach.entity.StaffActionEntity;
import com.sunsetbeach.model.ShiftChangeRequestChannel;
import com.sunsetbeach.model.ShiftChangeRequestType;
import com.sunsetbeach.notification.BotText;
import com.sunsetbeach.notification.NotificationAction.Kinds;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.service.ShiftChangeRequestService;
import com.sunsetbeach.service.ShiftChangeRequestService.Rejection;
import com.sunsetbeach.service.ShiftChangeRequestService.ShiftRequestException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Filing and withdrawing a shift request from "My shifts": Move (pick a day, then type the reason), Day off
 * (type the reason), Skip (when no reason is required), Cancel and Withdraw. The unfinished part - which shift,
 * which kind, which day - is a {@code BotConversation} in the database, not in memory; every step re-checks
 * the rules against the settings and the clock as they are <em>now</em>, so a dialogue left open past the
 * notice period fails cleanly instead of filing a request that should be refused.
 */
@Component
public class ShiftRequestActions implements BotActionHandler, BotDialogueHandler {

    static final String STATE = "SHIFT_REASON";

    private final RosterEntryRepository entryRepository;
    private final ShiftChangeRequestService requestService;
    private final BotConversations conversations;

    public ShiftRequestActions(RosterEntryRepository entryRepository, ShiftChangeRequestService requestService, BotConversations conversations) {
        this.entryRepository = entryRepository;
        this.requestService = requestService;
        this.conversations = conversations;
    }

    // ---- buttons ---------------------------------------------------------------------------------------

    @Override
    public Set<String> kinds() {
        return Set.of(Kinds.SHIFT_MOVE, Kinds.SHIFT_MOVE_DATE, Kinds.SHIFT_DELETE, Kinds.SHIFT_REASON_SKIP, Kinds.DIALOGUE_CANCEL, Kinds.SHIFT_REQUEST_CANCEL);
    }

    @Override
    public List<BotReply> handle(BotContext ctx, StaffActionEntity action) {
        return switch (action.getKind()) {
            case Kinds.SHIFT_MOVE -> pickDate(ctx, action.getSubjectId());
            case Kinds.SHIFT_MOVE_DATE -> startReason(ctx, action.getSubjectId(), ShiftChangeRequestType.MOVE, parse(action.getArg()));
            case Kinds.SHIFT_DELETE -> startReason(ctx, action.getSubjectId(), ShiftChangeRequestType.DELETE, null);
            case Kinds.SHIFT_REASON_SKIP -> skipReason(ctx);
            case Kinds.DIALOGUE_CANCEL -> {
                conversations.clear(ctx.chatId());
                yield List.of(BotReply.text(ctx.t(Key.BOT_CANCELLED)).toast(ctx.t(Key.BOT_CANCELLED)));
            }
            default -> withdraw(ctx, action.getSubjectId());
        };
    }

    private List<BotReply> pickDate(BotContext ctx, String entryId) {
        RosterEntryEntity entry = entryRepository.findById(entryId).orElse(null);
        if (entry == null) {
            return List.of(BotReply.toastOnly(ctx.t(Key.BOT_BUTTON_EXPIRED)));
        }
        Optional<Rejection> problem = requestService.check(ctx.user(), entry, ShiftChangeRequestType.MOVE);
        if (problem.isPresent()) {
            return refused(ctx, problem.get());
        }
        // Every eligible day up to moveMaxDaysAhead; the nearest moveWindowDays of them are buttons, the rest are
        // reached through "Other date..." (the calendar, or a typed date).
        List<LocalDate> targets = requestService.eligibleTargets(ctx.user(), entry);
        LocalDate lastButtonDay = ctx.today().plusDays(ctx.settings().getMoveWindowDays());
        List<LocalDate> buttons = targets.stream().filter(d -> !d.isAfter(lastButtonDay)).toList();
        boolean moreBeyond = targets.stream().anyMatch(d -> d.isAfter(lastButtonDay));
        if (targets.isEmpty()) {
            return List.of(BotReply.text(ctx.t(Key.MOVE_NO_DATES)));
        }
        BotReply reply = BotReply.text(ctx.t(Key.MOVE_PICK, Map.of("date", BotText.date(entry.getDate(), ctx.language()))));
        List<BotReply.Button> row = new ArrayList<>();
        for (LocalDate target : buttons) {
            row.add(new BotReply.Button(BotText.date(target, ctx.language()), Kinds.SHIFT_MOVE_DATE, entryId, target.toString()));
            if (row.size() == 3) {
                reply.row(row);
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()) {
            reply.row(row);
        }
        if (moreBeyond) {
            reply.row(BotReply.Button.of(ctx.t(Key.MOVE_OTHER_DATE), Kinds.SHIFT_CAL, entryId));
        }
        reply.row(BotReply.Button.of(ctx.t(Key.BTN_CANCEL), Kinds.DIALOGUE_CANCEL, null));
        return List.of(reply);
    }

    List<BotReply> startReason(BotContext ctx, String entryId, ShiftChangeRequestType type, LocalDate target) {
        RosterEntryEntity entry = entryRepository.findById(entryId).orElse(null);
        if (entry == null) {
            return List.of(BotReply.toastOnly(ctx.t(Key.BOT_BUTTON_EXPIRED)));
        }
        Optional<Rejection> problem = requestService.check(ctx.user(), entry, type);
        if (problem.isEmpty() && type == ShiftChangeRequestType.MOVE) {
            problem = requestService.checkTarget(ctx.user(), entry, target);
        }
        if (problem.isPresent()) {
            return refused(ctx, problem.get());
        }
        conversations.start(ctx.chatId(), ctx.user().getId(), STATE, entryId, type.getValue(), target);
        boolean required = ctx.settings().isReasonRequired();
        BotReply ask = BotReply.text(ctx.t(required ? Key.ASK_REASON : Key.ASK_REASON_OPTIONAL));
        if (!required) {
            ask.row(BotReply.Button.of(ctx.t(Key.BTN_SKIP), Kinds.SHIFT_REASON_SKIP, null));
        }
        ask.row(BotReply.Button.of(ctx.t(Key.BTN_CANCEL), Kinds.DIALOGUE_CANCEL, null));
        return List.of(ask);
    }

    private List<BotReply> skipReason(BotContext ctx) {
        Optional<BotConversationEntity> conversation = conversations.current(ctx.chatId());
        if (conversation.isEmpty() || !STATE.equals(conversation.get().getState()) || !conversation.get().getUserId().equals(ctx.user().getId())) {
            return List.of(BotReply.toastOnly(ctx.t(Key.BOT_BUTTON_EXPIRED)));
        }
        return submit(ctx, conversation.get(), null);
    }

    private List<BotReply> withdraw(BotContext ctx, String requestId) {
        var result = requestService.cancel(requestId, ctx.user(), ShiftChangeRequestChannel.TELEGRAM);
        ShiftChangeRequestEntity r = result.request();
        if (result.changedNow()) {
            String text = ctx.t(Key.REQUEST_WITHDRAWN, Map.of("what", requestService.describe(r, ctx.language())));
            return List.of(BotReply.text(text).toast(text));
        }
        String status = ctx.t(ShiftBotSupport.statusKey(r.getStatus()));
        return List.of(BotReply.toastOnly(ctx.t(Key.TOAST_ALREADY, Map.of("status", status))));
    }

    // ---- the reason, typed -------------------------------------------------------------------------------

    @Override
    public String state() {
        return STATE;
    }

    @Override
    public List<BotReply> onText(BotContext ctx, BotConversationEntity conversation, String text) {
        if (text.strip().length() > ShiftChangeRequestService.MAX_REASON_LENGTH) {
            return List.of(BotReply.text(ctx.t(Key.REASON_TOO_LONG)));
        }
        return submit(ctx, conversation, text);
    }

    private List<BotReply> submit(BotContext ctx, BotConversationEntity conversation, String reason) {
        conversations.clear(ctx.chatId());
        ShiftChangeRequestType type = ShiftChangeRequestType.fromValue(conversation.getRequestType());
        try {
            ShiftChangeRequestEntity request = requestService.submit(
                    ctx.user(), conversation.getEntryId(), type, conversation.getTargetDate(), reason, ShiftChangeRequestChannel.TELEGRAM);
            return List.of(BotReply.text(ctx.t(Key.REQUEST_SENT, Map.of("what", requestService.describe(request, ctx.language())))));
        } catch (ShiftRequestException e) {
            return refused(ctx, e.reason());
        }
    }

    private List<BotReply> refused(BotContext ctx, Rejection reason) {
        String text = ShiftBotSupport.rejectionText(ctx, reason);
        return List.of(BotReply.text(text).toast(text));
    }

    private static LocalDate parse(String iso) {
        try {
            return iso == null ? null : LocalDate.parse(iso);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
