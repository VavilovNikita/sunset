package com.sunsetbeach.tuya;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * What a person at a screen may read about a Tuya failure. The technical wording (environment
 * variable names, "subscribe on iot.tuya.com", DEPLOY.md) is for the administrator who can act on
 * it; everyone else gets one short sentence. Applied where a message leaves the server - the
 * error handler, the device list and the reservation panel - never where it is logged or audited.
 */
public final class TuyaMessages {

    public static final String NOT_SET_UP = "Gym equipment control isn't set up yet";

    private static final String[] TECHNICAL_MARKERS = {"TUYA_", "DEPLOY", "iot.tuya.com", "Tuya integration", "IoT Core", "Tuya project"};

    private TuyaMessages() {}

    /** The message as the current caller may see it: unchanged for an administrator, shortened for anyone else. */
    public static String forViewer(String message) {
        if (message == null || isAdmin() || !isTechnical(message)) {
            return message;
        }
        return NOT_SET_UP;
    }

    static boolean isTechnical(String message) {
        for (String marker : TECHNICAL_MARKERS) {
            if (message.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
