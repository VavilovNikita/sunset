package com.sunsetbeach.notification;

/**
 * Something a recipient can do about a notification, named in the core's own terms - a kind, the thing it
 * concerns and an optional argument. The core stores it ({@code StaffAction}) and knows nothing about how
 * a channel shows it: the Telegram adapter turns each into a button whose callback data is just the stored
 * row's opaque id, and what pressing it does is decided by a handler registered for the {@code kind}.
 *
 * @param kind      one of {@link Kinds}, or a kind a later feature registers a handler for
 * @param subjectId the record the action is about (a request id), nullable
 * @param arg       a short extra value, nullable
 */
public record NotificationAction(String kind, String subjectId, String arg) {

    public static NotificationAction of(String kind, String subjectId) {
        return new NotificationAction(kind, subjectId, null);
    }

    /** The action kinds the staff bot understands today. A new feature adds its own next to these. */
    public static final class Kinds {

        public static final String SHIFT_REQUEST_APPROVE = "SHIFT_REQUEST_APPROVE";
        public static final String SHIFT_REQUEST_REJECT = "SHIFT_REQUEST_REJECT";
        public static final String SHIFT_REQUEST_CANCEL = "SHIFT_REQUEST_CANCEL";
        public static final String SHIFT_MOVE = "SHIFT_MOVE";
        public static final String SHIFT_MOVE_DATE = "SHIFT_MOVE_DATE";
        public static final String SHIFT_DELETE = "SHIFT_DELETE";
        public static final String SHIFT_REASON_SKIP = "SHIFT_REASON_SKIP";
        public static final String DIALOGUE_CANCEL = "DIALOGUE_CANCEL";
        public static final String SET_LANGUAGE = "SET_LANGUAGE";
        public static final String SET_EVE_REMINDER = "SET_EVE_REMINDER";
        public static final String HR_VACATION_SET = "HR_VACATION_SET";
        public static final String HR_VACATION_CLEAR = "HR_VACATION_CLEAR";
        public static final String TASK_START = "TASK_START";
        public static final String TASK_DONE = "TASK_DONE";
        public static final String SET_THERAPIST_MORNING = "SET_THERAPIST_MORNING";

        private Kinds() {
        }
    }
}
