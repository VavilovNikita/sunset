package com.sunsetbeach.bot;

/**
 * What a person's bot menu is built from changed (their language, or which screens exist): the "/" command list of
 * their chat - {@code userId} null means every linked chat - is out of date. Published inside the changing
 * transaction; {@link BotMenuRefresher} acts after it commits.
 */
public record BotMenuChanged(String userId) {
}
