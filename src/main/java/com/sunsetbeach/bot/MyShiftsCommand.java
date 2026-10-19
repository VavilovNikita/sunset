package com.sunsetbeach.bot;

import com.sunsetbeach.bot.BotReply.Button;
import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.ShiftChangeRequestEntity;
import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.model.ShiftChangeRequestType;
import com.sunsetbeach.notification.BotText;
import com.sunsetbeach.notification.NotificationAction;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftChangeRequestRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.service.ShiftChangeRequestService;
import com.sunsetbeach.service.ShiftLabels;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * "My shifts": the next {@code myShiftsDays} days of the employee's own roster - the hotel date ("today",
 * "tomorrow"), shift code, its times and the department - and, under a future shift the rules allow changing,
 * the Move / Day off buttons; a shift with a waiting request shows its status and a Withdraw button instead.
 */
@Component
public class MyShiftsCommand implements BotCommandHandler {

    private final RosterEntryRepository entryRepository;
    private final ShiftCodeRepository shiftCodeRepository;
    private final ShiftChangeRequestRepository requestRepository;
    private final ShiftChangeRequestService requestService;

    public MyShiftsCommand(
            RosterEntryRepository entryRepository,
            ShiftCodeRepository shiftCodeRepository,
            ShiftChangeRequestRepository requestRepository,
            ShiftChangeRequestService requestService) {
        this.entryRepository = entryRepository;
        this.shiftCodeRepository = shiftCodeRepository;
        this.requestRepository = requestRepository;
        this.requestService = requestService;
    }

    @Override
    public String command() {
        return "shifts";
    }

    @Override
    public Key label() {
        return Key.MENU_SHIFTS;
    }

    @Override
    public boolean enabled(StaffBotSettingsEntity settings) {
        return settings.isMyShiftsEnabled();
    }

    @Override
    public int order() {
        return 10;
    }

    @Override
    @Transactional(readOnly = true)
    public List<BotReply> handle(BotContext ctx, String arguments) {
        int days = ctx.settings().getMyShiftsDays();
        LocalDate last = ctx.today().plusDays(days - 1L);
        List<RosterEntryEntity> entries = entryRepository.findByEmployeeUserIdAndDateBetween(ctx.user().getId(), ctx.today(), last).stream()
                .sorted(Comparator.comparing(RosterEntryEntity::getDate)).toList();
        Map<String, String> days_ = Map.of("days", String.valueOf(days));
        if (entries.isEmpty()) {
            return List.of(BotReply.text(ctx.t(Key.SHIFTS_NONE, days_)));
        }
        Map<String, ShiftCodeEntity> codes = shiftCodeRepository.findAllById(entries.stream().map(RosterEntryEntity::getShiftCodeId).distinct().toList()).stream()
                .collect(Collectors.toMap(ShiftCodeEntity::getId, c -> c));
        Map<String, ShiftChangeRequestEntity> pending = new HashMap<>();
        requestRepository.findByRosterEntryIdInAndStatus(entries.stream().map(RosterEntryEntity::getId).toList(), "PENDING")
                .forEach(r -> pending.put(r.getRosterEntryId(), r));

        List<String> lines = new ArrayList<>();
        lines.add(ctx.t(Key.SHIFTS_TITLE, days_));
        BotReply reply = null;
        List<List<Button>> rows = new ArrayList<>();
        String area = ctx.user().getStaffArea() == null ? "" : " - " + ctx.user().getStaffArea().getValue();
        for (RosterEntryEntity entry : entries) {
            ShiftCodeEntity code = codes.get(entry.getShiftCodeId());
            ShiftChangeRequestEntity waiting = pending.get(entry.getId());
            String tag = entry.getDate().equals(ctx.today()) ? ctx.t(Key.TAG_TODAY)
                    : entry.getDate().equals(ctx.today().plusDays(1)) ? ctx.t(Key.TAG_TOMORROW) : "";
            String request = waiting == null ? "" : ctx.t(Key.SHIFT_REQUEST_TAG, Map.of("status", ctx.t(ShiftBotSupport.statusKey(waiting.getStatus()))));
            lines.add(ctx.t(Key.SHIFT_LINE, Map.of(
                    "date", BotText.date(entry.getDate(), ctx.language()),
                    "tag", tag,
                    "shift", BotText.shift(code.getCode(), ShiftLabels.times(code)),
                    "area", area,
                    "request", request)));

            String dateLabel = BotText.date(entry.getDate(), ctx.language());
            Map<String, String> dateParams = Map.of("date", dateLabel);
            List<Button> row = new ArrayList<>();
            if (waiting != null) {
                row.add(Button.of(ctx.t(Key.BTN_WITHDRAW, dateParams), NotificationAction.Kinds.SHIFT_REQUEST_CANCEL, waiting.getId()));
            } else {
                if (requestService.check(ctx.user(), entry, ShiftChangeRequestType.MOVE).isEmpty()) {
                    row.add(Button.of(ctx.t(Key.BTN_MOVE, dateParams), NotificationAction.Kinds.SHIFT_MOVE, entry.getId()));
                }
                if (requestService.check(ctx.user(), entry, ShiftChangeRequestType.DELETE).isEmpty()) {
                    row.add(Button.of(ctx.t(Key.BTN_DELETE, dateParams), NotificationAction.Kinds.SHIFT_DELETE, entry.getId()));
                }
            }
            if (!row.isEmpty()) {
                rows.add(row);
            }
        }
        reply = BotReply.text(String.join("\n", lines));
        rows.forEach(reply::row);
        return List.of(reply);
    }
}
