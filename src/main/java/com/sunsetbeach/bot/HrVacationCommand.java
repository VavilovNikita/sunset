package com.sunsetbeach.bot;

import com.sunsetbeach.entity.BotConversationEntity;
import com.sunsetbeach.entity.StaffActionEntity;
import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.notification.NotificationAction.Kinds;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.service.ShiftRequestDeciderService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * "My leave" for the HR director (and for an administrator while nobody holds the function): shows the leave,
 * sets it in two steps (first day, last day, typed) or clears it. While the leave lasts, requests go to the
 * administrators ({@link ShiftRequestDeciderService}). Dates are the hotel's calendar days.
 */
@Component
public class HrVacationCommand implements BotCommandHandler, BotActionHandler, BotDialogueHandler {

    private static final String FROM = "HR_VACATION_FROM";
    private static final String TO = "HR_VACATION_TO";
    private static final DateTimeFormatter DOTTED = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final ShiftRequestDeciderService deciderService;
    private final BotConversations conversations;

    public HrVacationCommand(ShiftRequestDeciderService deciderService, BotConversations conversations) {
        this.deciderService = deciderService;
        this.conversations = conversations;
    }

    @Override
    public String command() {
        return "vacation";
    }

    @Override
    public Key label() {
        return Key.MENU_VACATION;
    }

    @Override
    public boolean enabled(StaffBotSettingsEntity settings) {
        return settings.isRequestsEnabled();
    }

    @Override
    public boolean availableTo(UserEntity user, LocalDate today) {
        boolean holder = Arrays.asList(user.getJobFunctions()).contains("HR");
        return holder || (user.getRole() == Role.ADMIN && deciderService.activeHr().isPresent());
    }

    @Override
    public int order() {
        return 70;
    }

    @Override
    public List<BotReply> handle(BotContext ctx, String arguments) {
        return List.of(screen(ctx));
    }

    private BotReply screen(BotContext ctx) {
        UserEntity hr = deciderService.activeHr().orElse(null);
        if (hr == null) {
            return BotReply.text(ctx.t(Key.VAC_NO_HR));
        }
        String text = hr.getHrVacationFrom() == null
                ? ctx.t(Key.VAC_NONE)
                : ctx.t(Key.VAC_STATUS, Map.of("from", hr.getHrVacationFrom().toString(), "to", hr.getHrVacationTo().toString()));
        BotReply reply = BotReply.text(text).row(BotReply.Button.of(ctx.t(Key.BTN_VAC_SET), Kinds.HR_VACATION_SET, null));
        if (hr.getHrVacationFrom() != null) {
            reply.row(BotReply.Button.of(ctx.t(Key.BTN_VAC_CLEAR), Kinds.HR_VACATION_CLEAR, null));
        }
        return reply;
    }

    // ---- buttons -----------------------------------------------------------------------------------------

    @Override
    public Set<String> kinds() {
        return Set.of(Kinds.HR_VACATION_SET, Kinds.HR_VACATION_CLEAR);
    }

    @Override
    public List<BotReply> handle(BotContext ctx, StaffActionEntity action) {
        if (!ctx.settings().isRequestsEnabled() || !availableTo(ctx.user(), ctx.today())) {
            throw new ForbiddenException("Not allowed");
        }
        if (Kinds.HR_VACATION_CLEAR.equals(action.getKind())) {
            try {
                deciderService.setVacation(ctx.user(), null, null, "Telegram");
            } catch (ConflictException e) {
                return List.of(BotReply.text(ctx.t(Key.VAC_NO_HR)));
            }
            return List.of(BotReply.text(ctx.t(Key.VAC_CLEARED)).toast(ctx.t(Key.VAC_CLEARED)));
        }
        conversations.start(ctx.chatId(), ctx.user().getId(), FROM, null, null, null);
        return List.of(BotReply.text(ctx.t(Key.VAC_ASK_FROM)));
    }

    // ---- the typed dates -----------------------------------------------------------------------------------

    @Override
    public String state() {
        return FROM;
    }

    /** One dialogue handler can't answer two states; {@link VacationEndDialogue} takes the second. */
    @Override
    public List<BotReply> onText(BotContext ctx, BotConversationEntity conversation, String text) {
        LocalDate from = parse(text);
        if (from == null) {
            return List.of(BotReply.text(ctx.t(Key.VAC_BAD_DATE)));
        }
        conversations.start(ctx.chatId(), ctx.user().getId(), TO, null, null, from);
        return List.of(BotReply.text(ctx.t(Key.VAC_ASK_TO)));
    }

    /** The second step. */
    @Component
    public static class VacationEndDialogue implements BotDialogueHandler {

        private final ShiftRequestDeciderService deciderService;
        private final BotConversations conversations;

        public VacationEndDialogue(ShiftRequestDeciderService deciderService, BotConversations conversations) {
            this.deciderService = deciderService;
            this.conversations = conversations;
        }

        @Override
        public String state() {
            return TO;
        }

        @Override
        public List<BotReply> onText(BotContext ctx, BotConversationEntity conversation, String text) {
            LocalDate to = parse(text);
            LocalDate from = conversation.getTargetDate();
            if (to == null || from == null || to.isBefore(from)) {
                return List.of(BotReply.text(ctx.t(Key.VAC_BAD_DATE)));
            }
            conversations.clear(ctx.chatId());
            try {
                deciderService.setVacation(ctx.user(), from, to, "Telegram");
            } catch (ConflictException e) {
                return List.of(BotReply.text(ctx.t(Key.VAC_NO_HR)));
            } catch (BadRequestException | ForbiddenException e) {
                return List.of(BotReply.text(ctx.t(Key.BOT_NOT_ALLOWED)));
            }
            return List.of(BotReply.text(ctx.t(Key.VAC_SAVED, Map.of("from", from.toString(), "to", to.toString()))));
        }
    }

    /** {@code 2026-11-03} or {@code 03.11.2026}. */
    static LocalDate parse(String text) {
        String value = text.strip();
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            try {
                return LocalDate.parse(value, DOTTED);
            } catch (DateTimeParseException e2) {
                return null;
            }
        }
    }
}
