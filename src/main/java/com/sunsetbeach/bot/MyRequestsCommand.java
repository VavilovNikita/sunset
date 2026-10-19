package com.sunsetbeach.bot;

import com.sunsetbeach.entity.ShiftChangeRequestEntity;
import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.notification.BotText;
import com.sunsetbeach.notification.NotificationAction;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.repository.ShiftChangeRequestRepository;
import com.sunsetbeach.service.ShiftChangeRequestService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

/** "My requests": the employee's latest shift requests with their status; a waiting one can be withdrawn. */
@Component
public class MyRequestsCommand implements BotCommandHandler {

    private static final int SHOWN = 10;

    private final ShiftChangeRequestRepository requestRepository;
    private final ShiftChangeRequestService requestService;

    public MyRequestsCommand(ShiftChangeRequestRepository requestRepository, ShiftChangeRequestService requestService) {
        this.requestRepository = requestRepository;
        this.requestService = requestService;
    }

    @Override
    public String command() {
        return "requests";
    }

    @Override
    public Key label() {
        return Key.MENU_REQUESTS;
    }

    @Override
    public boolean enabled(StaffBotSettingsEntity settings) {
        return settings.isMyRequestsEnabled() && settings.isRequestsEnabled();
    }

    @Override
    public int order() {
        return 40;
    }

    @Override
    public List<BotReply> handle(BotContext ctx, String arguments) {
        List<ShiftChangeRequestEntity> requests = requestRepository.findByEmployeeUserIdOrderByCreatedAtDesc(ctx.user().getId(), PageRequest.of(0, SHOWN));
        if (requests.isEmpty()) {
            return List.of(BotReply.text(ctx.t(Key.REQUESTS_NONE)));
        }
        List<String> lines = new ArrayList<>();
        lines.add(ctx.t(Key.REQUESTS_TITLE));
        List<BotReply.Button> withdraw = new ArrayList<>();
        for (ShiftChangeRequestEntity r : requests) {
            lines.add("- " + ctx.t(Key.REQUEST_LINE, Map.of(
                    "what", requestService.describe(r, ctx.language()),
                    "status", ctx.t(ShiftBotSupport.statusKey(r.getStatus())))));
            if ("PENDING".equals(r.getStatus())) {
                withdraw.add(BotReply.Button.of(
                        ctx.t(Key.BTN_WITHDRAW, Map.of("date", BotText.date(r.getShiftDate(), ctx.language()))),
                        NotificationAction.Kinds.SHIFT_REQUEST_CANCEL, r.getId()));
            }
        }
        BotReply reply = BotReply.text(String.join("\n", lines));
        withdraw.forEach(b -> reply.row(b));
        return List.of(reply);
    }
}
