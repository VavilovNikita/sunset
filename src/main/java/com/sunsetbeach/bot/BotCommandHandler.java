package com.sunsetbeach.bot;

import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.notification.NotificationMessages.Key;
import java.time.LocalDate;
import java.util.List;

/**
 * One command of the staff bot ({@code /shifts}, a menu button). A feature is added by writing a Spring
 * bean that implements this - the menu, the "/" command list, the on/off switch and the access check all
 * come from the methods below, so a new role screen (an engineer's tasks, a therapist's procedures, "who is
 * on shift" for a manager) is one more bean and no change to the dispatcher.
 */
public interface BotCommandHandler {

    /** The command without the slash, lower case: {@code shifts}. */
    String command();

    /** The key of the menu button's text; the button is also what a person can tap instead of typing the command. */
    Key label();

    /** Whether the administrator has this function switched on. */
    boolean enabled(StaffBotSettingsEntity settings);

    /** Whether this person is meant to see it at all (a role, a job function, being a decider today). */
    default boolean availableTo(UserEntity user, LocalDate today) {
        return true;
    }

    /** Menu order, ascending. */
    default int order() {
        return 100;
    }

    List<BotReply> handle(BotContext context, String arguments);
}
