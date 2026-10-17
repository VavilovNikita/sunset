package com.sunsetbeach.lighting;

import java.util.List;

/**
 * WLED's local JSON API over plain HTTP, as seen by the service: a thin interface so tests fake it
 * and never touch a network. Every method resolves and checks the address itself (see {@link
 * LanAddressGuard}) and throws {@link WledException} - never anything carrying a device's response.
 */
public interface WledClient {

    /** {@code GET /json/info}. */
    WledInfo info(String host, int port);

    /** {@code GET /json/state}. */
    WledState state(String host, int port);

    /** {@code GET /presets.json}: the presets stored on the controller, by id. */
    List<WledPreset> presets(String host, int port);

    /** {@code POST /json/state} with only the fields that are set. */
    void setState(String host, int port, WledStateChange change);

    /** What {@code /json/info} says: {@code name}, {@code ver}, {@code leds.count}, {@code mac}. */
    record WledInfo(String name, String version, int ledCount, String mac) {}

    /** {@code on}, {@code bri} (0-255) and {@code ps} (null when WLED reports none, i.e. -1). */
    record WledState(boolean on, int brightness, Integer presetId) {}

    record WledPreset(int id, String name) {}

    /** The fields of a command; null means "leave it". */
    record WledStateChange(Boolean on, Integer brightness, Integer presetId) {}
}
