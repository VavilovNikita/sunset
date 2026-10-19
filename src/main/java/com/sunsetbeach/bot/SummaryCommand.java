package com.sunsetbeach.bot;

import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.service.StaffSummaryService;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * "Summary" for MANAGER+: the hotel's day in a few lines - arrivals, departures, the house, occupancy, rooms, staff and
 * waiting shift requests - exactly the message the directors get each morning. Where each figure comes from, and which
 * ones are deliberately absent, is in {@link StaffSummaryService}; the blocks shown are the ones the settings switch on.
 */
@Component
public class SummaryCommand implements BotCommandHandler {

    private final StaffSummaryService summaryService;

    public SummaryCommand(StaffSummaryService summaryService) {
        this.summaryService = summaryService;
    }

    @Override
    public String command() {
        return "summary";
    }

    @Override
    public Key label() {
        return Key.MENU_SUMMARY;
    }

    @Override
    public boolean enabled(StaffBotSettingsEntity settings) {
        return settings.isSummaryCommandEnabled();
    }

    @Override
    public boolean availableTo(UserEntity user, LocalDate today) {
        return BotAccess.managerPlus(user);
    }

    @Override
    public boolean forEveryone() {
        return false;
    }

    @Override
    public int order() {
        return 88;
    }

    @Override
    public List<BotReply> handle(BotContext ctx, String arguments) {
        return List.of(BotReply.text(summaryService.render(summaryService.collect(ctx.settings()), ctx.language())));
    }
}
