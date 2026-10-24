package com.sunsetbeach.tuya;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class TuyaMessagesTests {

    private static final String TECHNICAL = "Tuya integration is not set up — TUYA_CLIENT_ID, TUYA_CLIENT_SECRET and TUYA_API_BASE are not set on the server";

    @AfterEach
    void clear() {
        SecurityContextHolder.clearContext();
    }

    private static void as(String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("u", "n", java.util.List.of(new SimpleGrantedAuthority(role))));
    }

    @Test
    void anAdministrator_seesTheTechnicalWording() {
        as("ROLE_ADMIN");
        assertThat(TuyaMessages.forViewer(TECHNICAL)).isEqualTo(TECHNICAL);
    }

    @Test
    void everyoneElse_getsOneShortSentence() {
        as("ROLE_MANAGER");
        assertThat(TuyaMessages.forViewer(TECHNICAL)).isEqualTo("Gym equipment control isn't set up yet");
        as("ROLE_CASHIER");
        assertThat(TuyaMessages.forViewer(TuyaClient.MSG_API_NOT_SUBSCRIBED)).isEqualTo("Gym equipment control isn't set up yet");
    }

    @Test
    void anOrdinaryMessage_isLeftAlone_forAnyone() {
        as("ROLE_CASHIER");
        assertThat(TuyaMessages.forViewer(TuyaClient.MSG_NETWORK)).isEqualTo(TuyaClient.MSG_NETWORK);
    }
}
