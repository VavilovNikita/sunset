package com.sunsetbeach.bot;

import java.util.ArrayList;
import java.util.List;

/**
 * One message a handler wants sent back, in the bot's own terms - text, buttons described by
 * <em>what they do</em> (a kind, a subject, an argument), never by Telegram callback data. The
 * dispatcher stores each button as a {@code StaffAction} row and puts only that row's opaque id in
 * Telegram's callback data, so a handler never builds or parses one.
 */
public final class BotReply {

    /** @param kind an action kind some {@link BotActionHandler} is registered for */
    public record Button(String label, String kind, String subjectId, String arg) {

        public static Button of(String label, String kind, String subjectId) {
            return new Button(label, kind, subjectId, null);
        }

        /** A button that does nothing when pressed (a calendar's heading, weekday, empty day): no action row is stored for it. */
        public static Button inert(String label) {
            return new Button(label, null, null, null);
        }

        public boolean isInert() {
            return kind == null;
        }
    }

    private final String text;
    private final List<List<Button>> rows = new ArrayList<>();
    private boolean editCurrent;
    private boolean showMenu;
    private String toast;

    private BotReply(String text) {
        this.text = text;
    }

    /** A new message. */
    public static BotReply text(String text) {
        return new BotReply(text);
    }

    /** No message, only the short note shown on the pressed button. */
    public static BotReply toastOnly(String toast) {
        BotReply reply = new BotReply(null);
        reply.toast = toast;
        return reply;
    }

    public BotReply row(Button... buttons) {
        rows.add(List.of(buttons));
        return this;
    }

    public BotReply row(List<Button> buttons) {
        rows.add(List.copyOf(buttons));
        return this;
    }

    /** Rewrites the message the pressed button sat under instead of sending a new one. */
    public BotReply editCurrent() {
        this.editCurrent = true;
        return this;
    }

    /** Attaches the persistent menu keyboard to this message. */
    public BotReply withMenu() {
        this.showMenu = true;
        return this;
    }

    public BotReply toast(String toast) {
        this.toast = toast;
        return this;
    }

    public String text() {
        return text;
    }

    public List<List<Button>> rows() {
        return rows;
    }

    public boolean isEditCurrent() {
        return editCurrent;
    }

    public boolean isShowMenu() {
        return showMenu;
    }

    public String toast() {
        return toast;
    }
}
