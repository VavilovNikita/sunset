package com.sunsetbeach.service;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.NotificationType;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.TelegramClient.TelegramException;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.StaffPrincipal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Over real HTTP: who may call the notification endpoints (settings are ADMIN, everything else
 * MANAGER+), and the log's date filter, which is a hotel (Bangkok) day, not a UTC one.
 */
class NotificationAccessAndLogTests extends NotificationTestBase {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;

    private UserEntity admin;
    private UserEntity manager;
    private UserEntity cashier;
    private UserEntity waiter;

    @BeforeEach
    void people() {
        admin = user("Admin", Role.ADMIN);
        manager = user("Manager", Role.MANAGER);
        cashier = user("Cashier", Role.CASHIER);
        waiter = user("Waiter", Role.WAITER);
    }

    private MockHttpServletRequestBuilder as(MockHttpServletRequestBuilder request, UserEntity who) {
        return request.header("Authorization", "Bearer " + jwtService.issue(new StaffPrincipal(who.getId(), who.getEmail(), who.getRole())));
    }

    @Test
    void everyNotificationEndpointNeedsManager_andTheSettingsNeedAdmin() throws Exception {
        for (UserEntity below : List.of(cashier, waiter)) {
            mockMvc.perform(as(get("/notifications/status"), below)).andExpect(status().isForbidden());
            mockMvc.perform(as(get("/notifications/staff"), below)).andExpect(status().isForbidden());
            mockMvc.perform(as(get("/notifications/log"), below)).andExpect(status().isForbidden());
            mockMvc.perform(as(post("/notifications/links/code").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"x\"}"), below))
                    .andExpect(status().isForbidden());
            mockMvc.perform(as(delete("/notifications/links/x"), below)).andExpect(status().isForbidden());
            mockMvc.perform(as(put("/notifications/staff/x/language").contentType(MediaType.APPLICATION_JSON).content("{}"), below))
                    .andExpect(status().isForbidden());
        }
        mockMvc.perform(get("/notifications/status")).andExpect(status().isUnauthorized());

        mockMvc.perform(as(get("/notifications/status"), manager)).andExpect(status().isOk());
        mockMvc.perform(as(get("/notifications/staff"), manager)).andExpect(status().isOk());
        mockMvc.perform(as(get("/notifications/log"), manager)).andExpect(status().isOk());

        mockMvc.perform(as(get("/settings/notifications"), manager)).andExpect(status().isForbidden());
        mockMvc.perform(as(put("/settings/notifications").contentType(MediaType.APPLICATION_JSON).content("{}"), manager)).andExpect(status().isForbidden());
        mockMvc.perform(as(get("/settings/notifications"), admin)).andExpect(status().isOk());
    }

    @Test
    void status_saysWhetherTelegramIsConfigured_andOmitsTheBotNameUntilKnown() throws Exception {
        telegram.configured = false;
        mockMvc.perform(as(get("/notifications/status"), manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.telegramConfigured").value(false))
                .andExpect(jsonPath("$.botUsername").doesNotExist());

        telegram.configured = true;
        channelService.refreshBotUsername();
        mockMvc.perform(as(get("/notifications/status"), manager))
                .andExpect(jsonPath("$.telegramConfigured").value(true))
                .andExpect(jsonPath("$.botUsername").value("sunset_test_bot"));
    }

    @Test
    void aManagerCanListStaffAndLinkThem_withoutBeingAnAdmin() throws Exception {
        mockMvc.perform(as(get("/notifications/staff"), manager))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId=='" + waiter.getId() + "')].telegramLinked").value(false));

        mockMvc.perform(as(post("/notifications/links/code").contentType(MediaType.APPLICATION_JSON).content("{\"userId\":\"" + waiter.getId() + "\"}"), manager))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(startsWith("https://t.me/sunset_test_bot?start=")));

        mockMvc.perform(as(delete("/notifications/links/" + waiter.getId()), manager)).andExpect(status().isNotFound());
    }

    @Test
    void settingsBelowTheTerminalFloor_are400OnTheField() throws Exception {
        String body = """
                {"lateEnabled":true,"lateGraceMinutes":1,"absenceEnabled":true,"absenceMinutes":60,"blockCreatedEnabled":true,"blockLastDayEnabled":true,
                 "blockExpiredEnabled":true,"terminalAlertsEnabled":true,"terminalSilenceMinutes":15,"quietHoursEnabled":true,"quietStart":"22:00",
                 "quietEnd":"07:00","morningTime":"09:00","defaultLanguage":"EN","lateRecipientUserIds":[],"directorUserIds":[]}""";

        mockMvc.perform(as(put("/settings/notifications").contentType(MediaType.APPLICATION_JSON).content(body), admin))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.fieldErrors.lateGraceMinutes").exists());
    }

    // ---- the log ----------------------------------------------------------------------------------------

    private void raiseAt(String hotelTime, String key, UserEntity to) {
        clock.set(hotelTime);
        notificationService.raise(NotificationType.BLOCK_LAST_DAY, NotificationMessages.Key.BLOCK_LAST_DAY, List.of(to.getId()), Map.of("room", "7"), key);
    }

    @Test
    void theDateFilterIsABangkokDay_notAUtcOne() throws Exception {
        settings(s -> s.setQuietHoursEnabled(false));
        UserEntity person = user("Nok", Role.WAITER);
        raiseAt("2033-05-16T23:59:59", "L:late-on-16", person); // 16:59:59 UTC the 16th
        raiseAt("2033-05-17T00:00:00", "L:start-of-17", person); // 17:00:00 UTC the 16th: still Bangkok's 17th
        raiseAt("2033-05-17T02:00:00", "L:two-am-17", person); // 19:00 UTC the 16th
        raiseAt("2033-05-17T23:59:59", "L:end-of-17", person);
        raiseAt("2033-05-18T00:00:00", "L:start-of-18", person);

        mockMvc.perform(as(get("/notifications/log?from=2033-05-17&to=2033-05-17&limit=500"), manager))
                .andExpect(jsonPath("$[?(@.recipientUserId=='" + person.getId() + "')]", hasSize(3)));
        mockMvc.perform(as(get("/notifications/log?from=2033-05-16&to=2033-05-16&limit=500"), manager))
                .andExpect(jsonPath("$[?(@.recipientUserId=='" + person.getId() + "')]", hasSize(1)));
        mockMvc.perform(as(get("/notifications/log?from=2033-05-18&to=2033-05-18&limit=500"), manager))
                .andExpect(jsonPath("$[?(@.recipientUserId=='" + person.getId() + "')]", hasSize(1)));
        mockMvc.perform(as(get("/notifications/log?from=2033-05-18&to=2033-05-17"), manager)).andExpect(status().isBadRequest());
        mockMvc.perform(as(get("/notifications/log?from=17-05-2033"), manager)).andExpect(status().isBadRequest());
    }

    @Test
    void theLogFiltersByTypeAndDeliveryStatus_andShowsWhatWentWrongInOurWords() throws Exception {
        settings(s -> s.setQuietHoursEnabled(false));
        UserEntity linked = user("Linked", Role.WAITER);
        UserEntity unlinked = user("Unlinked", Role.WAITER);
        link(linked);
        raiseAt("2033-05-17T10:00", "F:ok", linked);
        raiseAt("2033-05-17T10:00", "F:nochannel", unlinked);
        deliveryService.runOnce();
        telegram.failure = c -> new TelegramException(TelegramException.Kind.REJECTED, "Telegram refused the request (HTTP 400)");
        raiseAt("2033-05-17T10:01", "F:failed", linked);
        deliveryService.runOnce();

        String day = "from=2033-05-17&to=2033-05-17&limit=500&";
        mockMvc.perform(as(get("/notifications/log?" + day + "deliveryStatus=SENT"), manager))
                .andExpect(jsonPath("$[?(@.recipientUserId=='" + linked.getId() + "')]", hasSize(1)));
        mockMvc.perform(as(get("/notifications/log?" + day + "deliveryStatus=FAILED"), manager))
                .andExpect(jsonPath("$[?(@.recipientUserId=='" + linked.getId() + "')].error").value("Telegram refused the request (HTTP 400)"));
        mockMvc.perform(as(get("/notifications/log?" + day + "deliveryStatus=NO_CHANNEL"), manager))
                .andExpect(jsonPath("$[?(@.recipientUserId=='" + unlinked.getId() + "')]", hasSize(1)))
                .andExpect(jsonPath("$[?(@.recipientUserId=='" + linked.getId() + "')]", hasSize(0)));
        mockMvc.perform(as(get("/notifications/log?" + day + "type=LATE"), manager))
                .andExpect(jsonPath("$[?(@.recipientUserId=='" + linked.getId() + "')]", hasSize(0)));
        mockMvc.perform(as(get("/notifications/log?" + day + "type=BLOCK_LAST_DAY&deliveryStatus=PENDING"), manager))
                .andExpect(jsonPath("$[?(@.recipientUserId=='" + linked.getId() + "')]", hasSize(0)));
    }

    @Test
    void aLogLimitIsHonoured() throws Exception {
        settings(s -> s.setQuietHoursEnabled(false));
        UserEntity person = user("Nok", Role.WAITER);
        raiseAt("2033-05-17T10:00", "N:1", person);
        raiseAt("2033-05-17T10:01", "N:2", person);
        raiseAt("2033-05-17T10:02", "N:3", person);

        mockMvc.perform(as(get("/notifications/log?from=2033-05-17&to=2033-05-17&limit=2"), manager)).andExpect(jsonPath("$", hasSize(2)));
    }
}
