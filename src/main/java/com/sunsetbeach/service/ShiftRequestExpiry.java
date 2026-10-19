package com.sunsetbeach.service;

import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.NotificationMessages.Key;
import java.util.Map;

/**
 * Why a shift request became {@code EXPIRED} with nothing applied. The code is what is stored
 * ({@code ShiftChangeRequest.expiredReason}); every reader words it from here in its own language, so the
 * database never holds prose in one language.
 */
public enum ShiftRequestExpiry {
    SHIFT_STARTED(Key.EXP_SHIFT_STARTED),
    ENTRY_CHANGED(Key.EXP_ENTRY_CHANGED),
    ENTRY_GONE(Key.EXP_ENTRY_GONE),
    TARGET_TAKEN(Key.EXP_TARGET_TAKEN),
    EMPLOYEE_INACTIVE(Key.EXP_EMPLOYEE_INACTIVE),
    LOCKED(Key.EXP_LOCKED),
    APPLY_FAILED(Key.EXP_APPLY_FAILED);

    private final Key key;

    ShiftRequestExpiry(Key key) {
        this.key = key;
    }

    public String text(NotificationLanguage language) {
        return NotificationMessages.render(key, language, Map.of());
    }

    /** The stored code to a reason, or null for something that is not one of ours. */
    public static ShiftRequestExpiry of(String code) {
        try {
            return code == null ? null : valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
