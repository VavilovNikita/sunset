package com.sunsetbeach.bot;

import com.sunsetbeach.entity.BotConversationEntity;
import java.util.List;

/**
 * Takes the next free-text message of an unfinished dialogue ({@code BotConversation}, kept in the database,
 * fifteen minutes). The dispatcher routes by {@link #state()}; any command typed meanwhile has already
 * discarded the dialogue, so a handler only sees text meant for it.
 */
public interface BotDialogueHandler {

    String state();

    List<BotReply> onText(BotContext context, BotConversationEntity conversation, String text);
}
