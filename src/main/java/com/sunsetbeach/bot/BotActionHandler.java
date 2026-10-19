package com.sunsetbeach.bot;

import com.sunsetbeach.entity.StaffActionEntity;
import java.util.List;
import java.util.Set;

/**
 * What pressing a button does. By the time {@code handle} runs the dispatcher has checked that the press
 * comes from the chat of the person the button was made for; the handler still re-checks what that person may
 * do <em>now</em> (a function taken away, an account deactivated, a request already settled) because a
 * button can be days old. Throw {@link com.sunsetbeach.error.ForbiddenException} for "not allowed": the
 * dispatcher answers it with the standard text.
 */
public interface BotActionHandler {

    /** The action kinds ({@code NotificationAction.Kinds} or a feature's own) this handler answers. */
    Set<String> kinds();

    List<BotReply> handle(BotContext context, StaffActionEntity action);
}
