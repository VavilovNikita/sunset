package com.sunsetbeach.bot;

import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.model.TodayShiftState;
import com.sunsetbeach.model.TodayShiftStatus;
import com.sunsetbeach.notification.BotText;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.service.AttendanceService;
import com.sunsetbeach.service.StaffBotSettingsService;
import com.sunsetbeach.service.TerminalHealthService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * "Who is on shift" for managers (and, if the setting allows, the HR director): the attendance board
 * ({@link AttendanceService#getTodayShiftBoard}, the {@code GET /attendance/today} derivation) in words, by department -
 * late, absent, on shift, still to come, finished. Nothing is derived here; a person's state is the board's row.
 *
 * <p><b>A silent terminal is said first, and the states are not shown.</b> When {@link TerminalHealthService} says the
 * punches can't be trusted (the same rule that stops the late/absence alerts) a person with no scan is not late, just not
 * seen - so the screen opens with that and lists only who is on the roster, instead of calling everybody late.
 */
@Component
public class WhoIsOnShiftCommand implements BotCommandHandler {

    private final AttendanceService attendanceService;
    private final TerminalHealthService terminalHealthService;
    private final StaffBotSettingsService settingsService;

    public WhoIsOnShiftCommand(AttendanceService attendanceService, TerminalHealthService terminalHealthService, StaffBotSettingsService settingsService) {
        this.attendanceService = attendanceService;
        this.terminalHealthService = terminalHealthService;
        this.settingsService = settingsService;
    }

    @Override
    public String command() {
        return "onshift";
    }

    @Override
    public Key label() {
        return Key.MENU_ONSHIFT;
    }

    @Override
    public boolean enabled(StaffBotSettingsEntity settings) {
        return settings.isWhoIsOnShiftEnabled();
    }

    /** MANAGER+; the HR director too while {@code whoIsOnShiftForHr} is on (they can already read attendance). */
    @Override
    public boolean availableTo(UserEntity user, LocalDate today) {
        return BotAccess.managerPlus(user) || (BotAccess.hasFunction(user, "HR") && settingsService.getEntity().isWhoIsOnShiftForHr());
    }

    @Override
    public boolean forEveryone() {
        return false;
    }

    @Override
    public int order() {
        return 86;
    }

    @Override
    public List<BotReply> handle(BotContext ctx, String arguments) {
        List<TodayShiftStatus> board = attendanceService.getTodayShiftBoard();
        TerminalHealthService.Health health = terminalHealthService.current();
        List<String> lines = new ArrayList<>();
        // First of all, before the title: nothing below can be read without it.
        if (!health.healthy()) {
            lines.add(health.noActiveTerminal()
                    ? ctx.t(Key.ONSHIFT_TERMINAL_NONE)
                    : ctx.t(Key.ONSHIFT_TERMINAL_SILENT, Map.of("names", String.join(", ", health.silentTerminalNames()))));
        }
        lines.add(ctx.t(Key.ONSHIFT_TITLE, Map.of("date", BotText.date(ctx.today(), ctx.language()))));
        if (board.isEmpty()) {
            lines.add(ctx.t(Key.ONSHIFT_NOBODY));
            return List.of(BotReply.text(String.join("\n", lines)));
        }
        Map<String, List<TodayShiftStatus>> byArea = new LinkedHashMap<>();
        board.stream()
                .sorted(Comparator.comparing((TodayShiftStatus s) -> areaName(s, ctx)).thenComparing(TodayShiftStatus::getEmployeeName))
                .forEach(s -> byArea.computeIfAbsent(areaName(s, ctx), k -> new ArrayList<>()).add(s));
        for (Map.Entry<String, List<TodayShiftStatus>> area : byArea.entrySet()) {
            lines.add("");
            lines.add(area.getKey());
            if (!health.healthy()) {
                lines.add(ctx.t(Key.ONSHIFT_ROSTER, Map.of("names", names(area.getValue(), null))));
                continue;
            }
            group(lines, ctx, Key.GROUP_LATE, area.getValue(), TodayShiftState.LATE);
            group(lines, ctx, Key.GROUP_MISSED, area.getValue(), TodayShiftState.MISSED);
            group(lines, ctx, Key.GROUP_ON_SHIFT, area.getValue(), TodayShiftState.ON_SHIFT);
            group(lines, ctx, Key.GROUP_COMING, area.getValue(),
                    TodayShiftState.NOT_YET_ARRIVED, TodayShiftState.SCHEDULED, TodayShiftState.ARRIVING_SOON, TodayShiftState.BETWEEN_SHIFTS);
            group(lines, ctx, Key.GROUP_FINISHED, area.getValue(), TodayShiftState.FINISHED);
        }
        return List.of(BotReply.text(String.join("\n", lines)));
    }

    private void group(List<String> lines, BotContext ctx, Key key, List<TodayShiftStatus> rows, TodayShiftState... states) {
        String names = names(rows, List.of(states));
        if (!names.isEmpty()) {
            lines.add(ctx.t(key, Map.of("names", names)));
        }
    }

    private static String names(List<TodayShiftStatus> rows, List<TodayShiftState> states) {
        return rows.stream().filter(r -> states == null || states.contains(r.getState())).map(TodayShiftStatus::getEmployeeName).collect(Collectors.joining(", "));
    }

    private static String areaName(TodayShiftStatus status, BotContext ctx) {
        StaffArea area = status.getStaffArea().isPresent() ? status.getStaffArea().get() : null;
        return area == null ? ctx.t(Key.ONSHIFT_AREA_NONE) : area.getValue();
    }
}
