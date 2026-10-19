package com.sunsetbeach.bot;

import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.model.AttendanceDaySummary;
import com.sunsetbeach.model.AttendancePunch;
import com.sunsetbeach.model.ShiftInterval;
import com.sunsetbeach.model.TodayShiftState;
import com.sunsetbeach.model.TodayShiftStatus;
import com.sunsetbeach.notification.BotText;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.service.AttendanceService;
import java.time.Clock;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * "My day": today's shift and the employee's own live punches from the terminal, so a person can see the
 * terminal counted them. Nothing is derived here - the state (on shift, late, missed ...) is the attendance
 * board's own row for this employee ({@link AttendanceService#getTodayShiftBoard}) and the punches and planned
 * intervals are the day's row of the same summary the admin screen reads, so the bot can never disagree with
 * the board about whether someone is late.
 */
@Component
public class MyDayCommand implements BotCommandHandler {

    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final AttendanceService attendanceService;
    private final Clock clock;

    public MyDayCommand(AttendanceService attendanceService, Clock clock) {
        this.attendanceService = attendanceService;
        this.clock = clock;
    }

    @Override
    public String command() {
        return "today";
    }

    @Override
    public Key label() {
        return Key.MENU_DAY;
    }

    @Override
    public boolean enabled(StaffBotSettingsEntity settings) {
        return settings.isMyDayEnabled();
    }

    @Override
    public int order() {
        return 20;
    }

    @Override
    public List<BotReply> handle(BotContext ctx, String arguments) {
        TodayShiftStatus board = attendanceService.getTodayShiftBoard().stream()
                .filter(row -> row.getEmployeeUserId().equals(ctx.user().getId())).findFirst().orElse(null);
        AttendanceDaySummary day = attendanceService.summary(ctx.user().getId(), ctx.today().getYear(), ctx.today().getMonthValue()).stream()
                .filter(d -> d.getDate().equals(ctx.today().toString())).findFirst().orElse(null);

        List<String> lines = new ArrayList<>();
        lines.add(ctx.t(Key.DAY_TITLE, Map.of("date", BotText.date(ctx.today(), ctx.language()))));
        String code = board != null ? board.getShiftCode().getCode() : day != null && day.getShiftCode().isPresent() ? day.getShiftCode().get().getCode() : null;
        if (code == null) {
            lines.add(ctx.t(Key.DAY_NO_SHIFT));
        } else {
            String times = day == null ? "" : day.getPlannedIntervals().stream().map(this::interval).collect(Collectors.joining(", "));
            lines.add(ctx.t(Key.DAY_SHIFT, Map.of("shift", BotText.shift(code, times.isEmpty() ? null : times))));
            if (board != null) {
                lines.add(ctx.t(Key.DAY_STATE, Map.of("state", ctx.t(stateKey(board.getState())))));
            }
        }
        List<AttendancePunch> punches = day == null ? List.of() : day.getPunches();
        if (code != null || !punches.isEmpty()) {
            lines.add(punches.isEmpty() ? ctx.t(Key.DAY_NO_PUNCHES) : ctx.t(Key.DAY_PUNCHES));
            for (AttendancePunch punch : punches) {
                String time = punch.getPunchAt().atZoneSameInstant(clock.getZone()).toLocalTime().format(HHMM);
                lines.add(time + " " + ctx.t(punch.getDirection() == com.sunsetbeach.model.PunchDirection.IN ? Key.DAY_IN : Key.DAY_OUT));
            }
        }
        return List.of(BotReply.text(String.join("\n", lines)));
    }

    private String interval(ShiftInterval i) {
        return i.getStartTime() + "-" + i.getEndTime();
    }

    private static Key stateKey(TodayShiftState state) {
        return switch (state) {
            case NOT_YET_ARRIVED -> Key.STATE_NOT_YET_ARRIVED;
            case ON_SHIFT -> Key.STATE_ON_SHIFT;
            case FINISHED -> Key.STATE_FINISHED;
            case SCHEDULED -> Key.STATE_SCHEDULED;
            case ARRIVING_SOON -> Key.STATE_ARRIVING_SOON;
            case LATE -> Key.STATE_LATE;
            case BETWEEN_SHIFTS -> Key.STATE_BETWEEN_SHIFTS;
            case MISSED -> Key.STATE_MISSED;
        };
    }
}
