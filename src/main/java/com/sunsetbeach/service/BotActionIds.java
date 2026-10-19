package com.sunsetbeach.service;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * Ids of {@code StaffAction} rows: 12 characters of URL-safe base64 (72 random bits), so a button's
 * callback data is "a:" plus this - far under Telegram's 64 bytes - and cannot be guessed by another chat.
 */
public final class BotActionIds {

    private static final SecureRandom RANDOM = new SecureRandom();

    private BotActionIds() {
    }

    public static String next() {
        byte[] bytes = new byte[9];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
