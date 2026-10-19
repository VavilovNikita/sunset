package com.sunsetbeach.bot;

import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.SpaAppointment;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.SpaAppointmentTreatment;
import com.sunsetbeach.notification.BotText;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.service.SpaAppointmentService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * "My procedures" for a therapist: their own procedures for today and the following days
 * ({@code therapistScheduleDays} in all) - the time, the duration, the treatments, the table and, when the guest has a
 * hotel booking, the room number. Never the guest's name. Cancelled procedures are not shown.
 *
 * <p>The day is {@link SpaAppointmentService#getSchedule} - the spa screen's own read - filtered to this person; there is no
 * query of the bot's own, so what a therapist sees is what the spa grid holds.
 */
@Component
public class TherapistScheduleCommand implements BotCommandHandler {

    private final SpaAppointmentService spaService;

    public TherapistScheduleCommand(SpaAppointmentService spaService) {
        this.spaService = spaService;
    }

    @Override
    public String command() {
        return "procedures";
    }

    @Override
    public Key label() {
        return Key.MENU_PROCEDURES;
    }

    @Override
    public boolean enabled(StaffBotSettingsEntity settings) {
        return settings.isTherapistScheduleEnabled();
    }

    @Override
    public boolean availableTo(UserEntity user, LocalDate today) {
        return BotAccess.hasFunction(user, "THERAPIST");
    }

    @Override
    public boolean forEveryone() {
        return false;
    }

    @Override
    public int order() {
        return 84;
    }

    @Override
    public List<BotReply> handle(BotContext ctx, String arguments) {
        List<String> lines = new ArrayList<>();
        lines.add(ctx.t(Key.PROC_TITLE));
        int total = 0;
        for (int i = 0; i < ctx.settings().getTherapistScheduleDays(); i++) {
            LocalDate day = ctx.today().plusDays(i);
            List<SpaAppointment> mine = mineOn(spaService, day, ctx.user().getId());
            total += mine.size();
            if (mine.isEmpty()) {
                lines.add(ctx.t(Key.PROC_DAY_NONE, Map.of("date", BotText.date(day, ctx.language()))));
                continue;
            }
            lines.add(BotText.date(day, ctx.language()));
            for (SpaAppointment a : mine) {
                String treatments = a.getTreatments().stream().map(SpaAppointmentTreatment::getTreatmentName).collect(Collectors.joining(", "));
                String room = a.getRoomUnitLabel() == null || a.getRoomUnitLabel().isBlank() ? "" : ctx.t(Key.PROC_ROOM, Map.of("room", a.getRoomUnitLabel()));
                lines.add(ctx.t(Key.PROC_LINE, Map.of(
                        "time", a.getStartTime(), "minutes", String.valueOf(a.getDurationMinutes()), "treatments", treatments,
                        "table", a.getTableLabel(), "room", room)));
            }
        }
        if (total == 0) {
            return List.of(BotReply.text(ctx.t(Key.PROC_NONE)));
        }
        return List.of(BotReply.text(String.join("\n", lines)));
    }

    /** A therapist's procedures of one day that still stand (not cancelled), earliest first. Shared with the morning message. */
    static List<SpaAppointment> mineOn(SpaAppointmentService spaService, LocalDate day, String therapistUserId) {
        return spaService.getSchedule(day).getAppointments().stream()
                .filter(a -> therapistUserId.equals(a.getTherapistUserId()) && a.getStatus() != SpaAppointmentStatus.CANCELLED)
                .sorted(Comparator.comparing(SpaAppointment::getStartTime))
                .toList();
    }
}
