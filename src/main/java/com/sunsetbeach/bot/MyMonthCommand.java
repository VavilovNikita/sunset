package com.sunsetbeach.bot;

import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.model.AttendanceDaySummary;
import com.sunsetbeach.model.ShiftInterval;
import com.sunsetbeach.notification.BotText;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.service.AttendanceService;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * "My month": days and hours planned and actually worked this month - the same per-day summary the admin
 * attendance screen shows ({@link AttendanceService#summary}), added up. Planned hours are the shift code's
 * intervals; worked hours are the paired punches, exactly as that summary pairs them.
 */
@Component
public class MyMonthCommand implements BotCommandHandler {

    private final AttendanceService attendanceService;

    public MyMonthCommand(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @Override
    public String command() {
        return "month";
    }

    @Override
    public Key label() {
        return Key.MENU_MONTH;
    }

    @Override
    public boolean enabled(StaffBotSettingsEntity settings) {
        return settings.isMyMonthEnabled();
    }

    @Override
    public int order() {
        return 30;
    }

    @Override
    public List<BotReply> handle(BotContext ctx, String arguments) {
        YearMonth month = YearMonth.from(ctx.today());
        int plannedDays = 0;
        long plannedMinutes = 0;
        int workedDays = 0;
        long workedMinutes = 0;
        for (AttendanceDaySummary day : attendanceService.summary(ctx.user().getId(), month.getYear(), month.getMonthValue())) {
            long planned = day.getPlannedIntervals().stream().mapToLong(MyMonthCommand::minutes).sum();
            if (!day.getPlannedIntervals().isEmpty()) {
                plannedDays++;
                plannedMinutes += planned;
            }
            if (day.getWorkedMinutes().isPresent() && day.getWorkedMinutes().get() > 0) {
                workedDays++;
                workedMinutes += day.getWorkedMinutes().get();
            }
        }
        return List.of(BotReply.text(ctx.t(Key.MONTH_SUMMARY, Map.of(
                "month", BotText.month(month, ctx.language()),
                "plannedDays", String.valueOf(plannedDays),
                "plannedHours", hours(plannedMinutes),
                "workedDays", String.valueOf(workedDays),
                "workedHours", hours(workedMinutes)))));
    }

    private static long minutes(ShiftInterval interval) {
        LocalTime start = LocalTime.parse(interval.getStartTime());
        LocalTime end = LocalTime.parse(interval.getEndTime());
        long m = ChronoUnit.MINUTES.between(start, end);
        return m < 0 ? m + 24 * 60 : m; // an interval that ends after midnight
    }

    private static String hours(long minutes) {
        return minutes % 60 == 0 ? String.valueOf(minutes / 60) : String.format(java.util.Locale.ROOT, "%.1f", minutes / 60.0);
    }
}
