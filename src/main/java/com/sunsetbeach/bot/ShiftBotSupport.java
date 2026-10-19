package com.sunsetbeach.bot;

import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.service.ShiftChangeRequestService.Rejection;
import java.util.Map;

/** Small mappings shared by the shift-related bot features. */
final class ShiftBotSupport {

    private ShiftBotSupport() {
    }

    static Key statusKey(String status) {
        return switch (status) {
            case "APPROVED" -> Key.STATUS_APPROVED;
            case "REJECTED" -> Key.STATUS_REJECTED;
            case "CANCELLED" -> Key.STATUS_CANCELLED;
            case "EXPIRED" -> Key.STATUS_EXPIRED;
            default -> Key.STATUS_PENDING;
        };
    }

    static Key rejectionKey(Rejection reason) {
        return switch (reason) {
            case DISABLED -> Key.REJ_DISABLED;
            case NOT_YOURS -> Key.REJ_NOT_YOURS;
            case NOT_A_SHIFT -> Key.REJ_NOT_A_SHIFT;
            case LOCKED -> Key.REJ_LOCKED;
            case PAST -> Key.REJ_PAST;
            case TODAY -> Key.REJ_TODAY;
            case TOO_LATE -> Key.REJ_TOO_LATE;
            case PENDING -> Key.REJ_PENDING;
            case TARGET -> Key.REJ_TARGET;
            case TARGET_BUSY -> Key.REJ_TARGET_BUSY;
            case TARGET_PAST -> Key.REJ_TARGET_PAST;
            case TARGET_TOO_FAR -> Key.REJ_TARGET_FAR;
            case TARGET_SAME_DAY -> Key.REJ_TARGET_SAME;
            case TARGET_TOO_SOON -> Key.REJ_TARGET_SOON;
            case REASON_REQUIRED -> Key.REJ_REASON_REQUIRED;
            case REASON_TOO_LONG -> Key.REASON_TOO_LONG;
        };
    }

    /** The refusal in the person's language, with the configured numbers filled in. */
    static String rejectionText(BotContext ctx, Rejection reason) {
        return ctx.t(rejectionKey(reason), Map.of(
                "hours", String.valueOf(ctx.settings().getRequestMinHoursBefore()),
                "days", String.valueOf(ctx.settings().getMoveMaxDaysAhead())));
    }
}
