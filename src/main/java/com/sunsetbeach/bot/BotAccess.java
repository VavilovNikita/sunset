package com.sunsetbeach.bot;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.Role;
import java.util.Arrays;

/**
 * Who may use a role screen, asked of the person's current record every time (a menu, a typed command, a pressed
 * button). The role half uses explicit constants - never {@code ordinal()}, {@code Role} is generated from the spec -
 * and the function half is a plain membership test: a job function is a second axis and never climbs the role
 * ladder (see {@code SecurityConfig#engineerOrManagerPlus}, whose rule {@link #engineerOrManagerPlus} repeats).
 */
final class BotAccess {

    private BotAccess() {
    }

    static boolean managerPlus(UserEntity user) {
        return user != null && user.isActive() && (user.getRole() == Role.MANAGER || user.getRole() == Role.ADMIN);
    }

    static boolean hasFunction(UserEntity user, String function) {
        return user != null && user.isActive() && user.getJobFunctions() != null && Arrays.asList(user.getJobFunctions()).contains(function);
    }

    /** The ENGINEER function, or MANAGER and above as the fallback - the same rule as {@code PATCH /maintenance-tasks/{id}/status}. */
    static boolean engineerOrManagerPlus(UserEntity user) {
        return hasFunction(user, "ENGINEER") || managerPlus(user);
    }
}
