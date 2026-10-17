package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.NotificationType;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.notification.NotificationMessages;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** The nightly clean-up: older than 90 days goes (deliveries with it), the rest stays. Time is the test clock. */
class NotificationRetentionTests extends NotificationTestBase {

    @Autowired private NotificationRetentionService retention;

    private void raiseAt(String hotelTime, String key, UserEntity to) {
        clock.set(hotelTime);
        notificationService.raise(NotificationType.BLOCK_LAST_DAY, NotificationMessages.Key.BLOCK_LAST_DAY, List.of(to.getId()), Map.of("room", "1"), key);
    }

    @Test
    void removesNotificationsAndDeliveriesOlderThanTheWindow_andKeepsTheRest() {
        settings(s -> s.setQuietHoursEnabled(false));
        UserEntity person = user("Nok", Role.WAITER);
        link(person);
        raiseAt("2033-01-01T10:00", "R:old", person); // 91 days before 2033-04-02
        raiseAt("2033-01-03T10:00", "R:edge", person); // 89 days
        raiseAt("2033-03-20T10:00", "R:new", person);

        clock.set("2033-04-02T10:00");
        retention.purge();

        assertThat(notificationsOf(person)).extracting("dedupKey").containsExactly("R:edge", "R:new");
        List<String> ids = notificationRepository.findAll().stream().map(n -> n.getId()).toList();
        assertThat(deliveryRepository.findAll()).allSatisfy(d -> assertThat(ids).contains(d.getNotificationId()));
        assertThat(deliveryRepository.findByNotificationIdIn(notificationsOf(person).stream().map(n -> n.getId()).toList())).hasSize(2);
    }

    @Test
    void exactlyNinetyDaysOld_isStillKept() {
        settings(s -> s.setQuietHoursEnabled(false));
        UserEntity person = user("Nok", Role.WAITER);
        raiseAt("2033-01-03T10:00", "R:exact", person);

        clock.set("2033-04-03T10:00");
        retention.purge();

        assertThat(notificationsOf(person)).hasSize(1);
        clock.set("2033-04-03T10:01");
        retention.purge();
        assertThat(notificationsOf(person)).isEmpty();
    }
}
