package com.sunsetbeach.bot;

import com.sunsetbeach.entity.ShiftChangeRequestEntity;
import com.sunsetbeach.entity.StaffActionEntity;
import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.ShiftChangeRequestChannel;
import com.sunsetbeach.notification.NotificationAction;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.repository.ShiftChangeRequestRepository;
import com.sunsetbeach.service.ShiftChangeRequestService;
import com.sunsetbeach.service.ShiftChangeRequestService.DecisionResult;
import com.sunsetbeach.service.ShiftRequestDeciderService;
import com.sunsetbeach.service.ShiftRequestExpiry;
import com.sunsetbeach.service.ShiftRequestMessageSync;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * "Requests" for whoever decides today (the HR director, or the administrators on the fallback): the waiting
 * shift requests, each its own message with Approve / Reject. The same two buttons come on the message a new
 * request is announced with ({@code NotificationAction}), and both go through {@link #handle(BotContext, StaffActionEntity)}.
 *
 * <p>Pressing a button decides through {@link ShiftChangeRequestService#decide}, which re-checks that the
 * person is one of today's deciders <em>now</em> and is idempotent: a request someone else settled meanwhile
 * answers with its current status and changes nothing.
 */
@Component
public class PendingRequestsCommand implements BotCommandHandler, BotActionHandler {

    private static final int SHOWN = 10;

    private final ShiftChangeRequestRepository requestRepository;
    private final ShiftChangeRequestService requestService;
    private final ShiftRequestDeciderService deciderService;
    private final ShiftRequestMessageSync messageSync;

    public PendingRequestsCommand(
            ShiftChangeRequestRepository requestRepository,
            ShiftChangeRequestService requestService,
            ShiftRequestDeciderService deciderService,
            ShiftRequestMessageSync messageSync) {
        this.requestRepository = requestRepository;
        this.requestService = requestService;
        this.deciderService = deciderService;
        this.messageSync = messageSync;
    }

    @Override
    public String command() {
        return "pending";
    }

    @Override
    public Key label() {
        return Key.MENU_PENDING;
    }

    @Override
    public boolean enabled(StaffBotSettingsEntity settings) {
        return settings.isRequestsEnabled();
    }

    @Override
    public boolean availableTo(UserEntity user, LocalDate today) {
        return deciderService.mayDecide(user, today) || deciderService.decidesTheHrDirectorsRequests(user);
    }

    @Override
    public boolean forEveryone() {
        return false;
    }

    @Override
    public int order() {
        return 60;
    }

    @Override
    public List<BotReply> handle(BotContext ctx, String arguments) {
        // Not the requests this person may not decide (the HR director's own goes to the administrators).
        List<ShiftChangeRequestEntity> pending = requestRepository.findByStatus("PENDING").stream()
                .filter(r -> deciderService.mayDecide(ctx.user(), r.getEmployeeUserId(), ctx.today()))
                .sorted(Comparator.comparing(ShiftChangeRequestEntity::getCreatedAt)).limit(SHOWN).toList();
        if (pending.isEmpty()) {
            return List.of(BotReply.text(ctx.t(Key.PENDING_NONE)));
        }
        List<BotReply> replies = new java.util.ArrayList<>();
        replies.add(BotReply.text(ctx.t(Key.PENDING_TITLE)));
        for (ShiftChangeRequestEntity r : pending) {
            replies.add(BotReply.text(requestService.summaryText(r, ctx.language())).row(
                    BotReply.Button.of(ctx.t(Key.BTN_APPROVE), NotificationAction.Kinds.SHIFT_REQUEST_APPROVE, r.getId()),
                    BotReply.Button.of(ctx.t(Key.BTN_REJECT), NotificationAction.Kinds.SHIFT_REQUEST_REJECT, r.getId())));
        }
        return replies;
    }

    @Override
    public Set<String> kinds() {
        return Set.of(NotificationAction.Kinds.SHIFT_REQUEST_APPROVE, NotificationAction.Kinds.SHIFT_REQUEST_REJECT);
    }

    @Override
    public List<BotReply> handle(BotContext ctx, StaffActionEntity action) {
        boolean approve = NotificationAction.Kinds.SHIFT_REQUEST_APPROVE.equals(action.getKind());
        DecisionResult result;
        try {
            result = requestService.decide(action.getSubjectId(), ctx.user(), approve, null, ShiftChangeRequestChannel.TELEGRAM);
        } catch (NotFoundException e) {
            return List.of(BotReply.toastOnly(ctx.t(Key.BOT_BUTTON_EXPIRED)));
        }
        // ForbiddenException (not a decider any more) goes up: the dispatcher answers it with the standard "not allowed".
        ShiftChangeRequestEntity r = result.request();
        String toast;
        if (result.changedNow() && "APPROVED".equals(r.getStatus())) {
            toast = ctx.t(Key.TOAST_APPROVED);
        } else if (result.changedNow() && "REJECTED".equals(r.getStatus())) {
            toast = ctx.t(Key.TOAST_REJECTED);
        } else if ("EXPIRED".equals(r.getStatus()) && r.getExpiredReason() != null) {
            ShiftRequestExpiry why = ShiftRequestExpiry.of(r.getExpiredReason());
            toast = why == null ? "" : why.text(ctx.language());
        } else {
            toast = ctx.t(Key.TOAST_ALREADY, Map.of("status", ctx.t(ShiftBotSupport.statusKey(r.getStatus()))));
        }
        BotReply reply = BotReply.toastOnly(toast);
        // A message made by the bot's own list isn't covered by ShiftRequestMessageSync (that rewrites the
        // announcements); settle it here. The announcement's own message was already rewritten, buttons gone.
        if (action.getNotificationId() == null && !"PENDING".equals(r.getStatus())) {
            return List.of(BotReply.text(requestService.summaryText(r, ctx.language()) + "\n\n" + messageSync.outcomeLine(r, ctx.language()))
                    .editCurrent().toast(toast));
        }
        return List.of(reply);
    }
}
