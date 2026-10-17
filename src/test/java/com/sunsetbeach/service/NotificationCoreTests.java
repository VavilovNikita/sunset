package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.sunsetbeach.entity.NotificationDeliveryEntity;
import com.sunsetbeach.entity.NotificationEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.NotificationType;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.QuietHours;
import com.sunsetbeach.notification.TelegramClient.TelegramException;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.Test;

/**
 * The core of the notification centre: what makes a repeat or a race harmless, where quiet hours cut,
 * and what a failing channel does to a delivery. Time is the test clock, never the wall clock.
 */
class NotificationCoreTests extends NotificationTestBase {

    private List<NotificationDeliveryEntity> deliveriesOf(UserEntity user) {
        List<String> ids = notificationsOf(user).stream().map(NotificationEntity::getId).toList();
        return deliveryRepository.findByNotificationIdIn(ids);
    }

    private int raise(String dedupKey, UserEntity... recipients) {
        return notificationService.raise(
                NotificationType.BLOCK_LAST_DAY,
                NotificationMessages.Key.BLOCK_LAST_DAY,
                List.of(recipients).stream().map(UserEntity::getId).toList(),
                Map.of("room", "101"),
                dedupKey);
    }

    @Test
    void sameDedupKeyTwice_notifiesOnce_andCreatesOneDelivery() {
        UserEntity person = user("Nok", Role.WAITER);
        link(person);

        assertThat(raise("K:1", person)).isEqualTo(1);
        assertThat(raise("K:1", person)).isZero();

        assertThat(notificationsOf(person)).hasSize(1);
        assertThat(deliveriesOf(person)).hasSize(1);
        // The same key for a different person is a different notification.
        UserEntity other = user("Pim", Role.WAITER);
        assertThat(raise("K:1", person, other)).isEqualTo(1);
        assertThat(notificationsOf(other)).hasSize(1);
    }

    @Test
    void manyThreadsRaisingTheSameKeyAtOnce_writeExactlyOneNotificationAndDelivery() throws Exception {
        UserEntity person = user("Nok", Role.WAITER);
        link(person);
        int threads = 8;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                results.add(pool.submit(() -> {
                    barrier.await();
                    return raise("RACE:1", person);
                }));
            }
            int raised = 0;
            for (Future<Integer> f : results) {
                raised += f.get();
            }
            assertThat(raised).isEqualTo(1);
        } finally {
            pool.shutdownNow();
        }
        assertThat(notificationsOf(person)).hasSize(1);
        assertThat(deliveriesOf(person)).hasSize(1);
    }

    @Test
    void anInactiveOrUnknownRecipient_isSkipped() {
        UserEntity gone = user("Gone", Role.WAITER);
        gone.setActive(false);
        userRepository.saveAndFlush(gone);

        assertThat(notificationService.raise(
                        NotificationType.BLOCK_LAST_DAY, NotificationMessages.Key.BLOCK_LAST_DAY, List.of(gone.getId(), "no-such-user"), Map.of("room", "1"), "K:skip"))
                .isZero();
    }

    @Test
    void textIsWrittenInTheRecipientsLanguage_orTheDefault() {
        UserEntity russian = user("Ivan", Role.WAITER);
        russian.setNotificationLanguage("RU");
        userRepository.saveAndFlush(russian);
        UserEntity thai = user("Somchai", Role.WAITER);
        thai.setNotificationLanguage("TH");
        userRepository.saveAndFlush(thai);
        UserEntity plain = user("Sam", Role.WAITER);
        settings(s -> s.setDefaultLanguage("RU"));

        raise("K:lang", russian, thai, plain);

        assertThat(notificationsOf(russian).get(0).getText()).contains("Комната 101");
        assertThat(notificationsOf(thai).get(0).getText()).contains("ห้อง 101");
        assertThat(notificationsOf(plain).get(0).getLanguage()).isEqualTo("RU");
        settings(s -> s.setDefaultLanguage("EN"));
        UserEntity english = user("Eve", Role.WAITER);
        raise("K:lang2", english);
        assertThat(notificationsOf(english).get(0).getText()).startsWith("Room 101: today is the last day");
    }

    @Test
    void aMessageRaisedWithoutAPath_hasNoLinkLine() {
        UserEntity person = user("Nok", Role.WAITER);
        raise("K:nolink", person);
        assertThat(notificationsOf(person).get(0).getText()).doesNotContain("{").doesNotContain("http");
    }

    // ---- quiet hours -------------------------------------------------------------------------------

    @Test
    void quietHours_pureBoundaries() {
        LocalTime start = LocalTime.of(22, 0);
        LocalTime end = LocalTime.of(7, 0);
        LocalDateTime d = LocalDateTime.parse("2033-05-17T00:00");

        assertThat(QuietHours.endOfQuietHours(d.withHour(21).withMinute(59).withSecond(59), start, end)).isEmpty();
        assertThat(QuietHours.endOfQuietHours(d.withHour(22), start, end)).contains(LocalDateTime.parse("2033-05-18T07:00"));
        assertThat(QuietHours.endOfQuietHours(d.withHour(23).withMinute(59), start, end)).contains(LocalDateTime.parse("2033-05-18T07:00"));
        assertThat(QuietHours.endOfQuietHours(d, start, end)).contains(LocalDateTime.parse("2033-05-17T07:00"));
        assertThat(QuietHours.endOfQuietHours(d.withHour(6).withMinute(59).withSecond(59), start, end)).contains(LocalDateTime.parse("2033-05-17T07:00"));
        assertThat(QuietHours.endOfQuietHours(d.withHour(7), start, end)).isEmpty();
        // A window inside one day, and a "window" with no length.
        assertThat(QuietHours.endOfQuietHours(d.withHour(2), LocalTime.of(1, 0), LocalTime.of(5, 0))).contains(LocalDateTime.parse("2033-05-17T05:00"));
        assertThat(QuietHours.endOfQuietHours(d.withHour(5), LocalTime.of(1, 0), LocalTime.of(5, 0))).isEmpty();
        assertThat(QuietHours.endOfQuietHours(d.withHour(3), start, start)).isEmpty();
    }

    @Test
    void raisedInQuietHours_isWrittenAtOnce_butDueOnlyAtSevenBangkok() {
        UserEntity person = user("Nok", Role.WAITER);
        String chat = link(person);

        clock.set("2033-05-17T22:00");
        raise("Q:night", person);

        assertThat(notificationsOf(person)).hasSize(1); // visible in the log straight away
        NotificationDeliveryEntity delivery = deliveriesOf(person).get(0);
        // 07:00 on the 18th in Bangkok is 00:00 UTC the same day.
        assertThat(delivery.getNextAttemptAt()).isEqualTo(LocalDateTime.parse("2033-05-18T00:00"));

        clock.set("2033-05-18T06:59:59");
        assertThat(deliveryService.runOnce()).isZero();
        assertThat(telegram.textsTo(chat)).isEmpty();

        clock.set("2033-05-18T07:00");
        assertThat(deliveryService.runOnce()).isEqualTo(1);
        assertThat(telegram.textsTo(chat)).hasSize(1);
    }

    @Test
    void justBeforeAndAtTheStartOfQuietHours_onlyTheLatterWaits() {
        UserEntity person = user("Nok", Role.WAITER);
        link(person);

        clock.set("2033-05-17T21:59:59");
        raise("Q:before", person);
        clock.set("2033-05-17T22:00");
        raise("Q:at", person);

        List<NotificationDeliveryEntity> deliveries = deliveriesOf(person);
        assertThat(deliveries).extracting(NotificationDeliveryEntity::getNextAttemptAt)
                .containsExactlyInAnyOrder(LocalDateTime.parse("2033-05-17T14:59:59"), LocalDateTime.parse("2033-05-18T00:00"));
    }

    @Test
    void eachMessageHeldOverNightGoesOutOnItsOwn_neverMergedIntoOne() {
        UserEntity person = user("Nok", Role.WAITER);
        String chat = link(person);
        clock.set("2033-05-17T23:00");
        raise("Q:a", person);
        raise("Q:b", person);
        raise("Q:c", person);

        clock.set("2033-05-18T07:00");
        deliveryService.runOnce();

        assertThat(telegram.textsTo(chat)).hasSize(3);
    }

    @Test
    void quietHoursOff_sendsAtOnce() {
        settings(s -> s.setQuietHoursEnabled(false));
        UserEntity person = user("Nok", Role.WAITER);
        String chat = link(person);
        clock.set("2033-05-17T23:30");

        raise("Q:off", person);
        deliveryService.runOnce();

        assertThat(telegram.textsTo(chat)).hasSize(1);
    }

    // ---- delivery ----------------------------------------------------------------------------------

    @Test
    void aRecipientWithNoLinkedChat_showsAsNoDeliveryAndNothingIsSent() {
        UserEntity person = user("Nok", Role.WAITER);
        raise("D:nolink", person);

        assertThat(deliveriesOf(person)).isEmpty();
        assertThat(deliveryService.runOnce()).isZero();
    }

    @Test
    void anUnreachableTelegram_isRetriedWithBackoff_thenFailsInOurOwnWords() {
        settings(s -> s.setQuietHoursEnabled(false));
        UserEntity person = user("Nok", Role.WAITER);
        String chat = link(person);
        telegram.failure = c -> new TelegramException(TelegramException.Kind.UNAVAILABLE, "Telegram did not answer");
        raise("D:retry", person);

        deliveryService.runOnce();
        NotificationDeliveryEntity first = deliveriesOf(person).get(0);
        assertThat(first.getStatus()).isEqualTo("PENDING");
        assertThat(first.getAttempts()).isEqualTo(1);
        assertThat(first.getLastError()).isEqualTo("Telegram did not answer");

        // Not due yet: nothing happens a second later.
        clock.set("2033-05-17T10:00:30");
        assertThat(deliveryService.runOnce()).isZero();

        clock.set("2033-05-17T10:01:01");
        deliveryService.runOnce();
        assertThat(deliveriesOf(person).get(0).getAttempts()).isEqualTo(2);

        clock.set("2033-05-17T10:04");
        deliveryService.runOnce();
        NotificationDeliveryEntity last = deliveriesOf(person).get(0);
        assertThat(last.getStatus()).isEqualTo("FAILED"); // app.notifications.max-attempts=3 in tests
        assertThat(last.getAttempts()).isEqualTo(3);
        assertThat(telegram.textsTo(chat)).isEmpty();
    }

    @Test
    void aTelegramThatComesBack_deliversTheRetry() {
        settings(s -> s.setQuietHoursEnabled(false));
        UserEntity person = user("Nok", Role.WAITER);
        String chat = link(person);
        telegram.failure = c -> new TelegramException(TelegramException.Kind.UNAVAILABLE, "Telegram did not answer");
        raise("D:back", person);
        deliveryService.runOnce();

        telegram.failure = c -> null;
        clock.set("2033-05-17T10:02");
        deliveryService.runOnce();

        assertThat(deliveriesOf(person).get(0).getStatus()).isEqualTo("SENT");
        assertThat(deliveriesOf(person).get(0).getLastError()).isNull();
        assertThat(telegram.textsTo(chat)).hasSize(1);
    }

    @Test
    void aRetryDueDuringQuietHours_waitsForTheMorning() {
        UserEntity person = user("Nok", Role.WAITER);
        link(person);
        clock.set("2033-05-17T21:59");
        raise("D:quietretry", person);
        telegram.failure = c -> new TelegramException(TelegramException.Kind.UNAVAILABLE, "Telegram did not answer");
        deliveryService.runOnce();

        clock.set("2033-05-17T22:00:30"); // the one-minute retry now falls into quiet hours
        deliveryService.runOnce();

        assertThat(deliveriesOf(person).get(0).getNextAttemptAt()).isEqualTo(LocalDateTime.parse("2033-05-18T00:00"));
    }

    @Test
    void aBlockedBot_dropsTheLink_andFailsTheDeliveryWithoutRetrying() {
        settings(s -> s.setQuietHoursEnabled(false));
        UserEntity person = user("Nok", Role.WAITER);
        String chat = link(person);
        telegram.failure = c -> new TelegramException(TelegramException.Kind.BLOCKED, "The recipient blocked the bot");
        raise("D:blocked", person);

        deliveryService.runOnce();

        NotificationDeliveryEntity d = deliveriesOf(person).get(0);
        assertThat(d.getStatus()).isEqualTo("FAILED");
        assertThat(d.getAttempts()).isEqualTo(1);
        assertThat(linkRepository.findByUserIdAndChannel(person.getId(), "TELEGRAM")).isEmpty();
        assertThat(telegram.textsTo(chat)).isEmpty();
    }

    @Test
    void withoutABotToken_nothingIsSent_andTheMessagesStayPending() {
        settings(s -> s.setQuietHoursEnabled(false));
        UserEntity person = user("Nok", Role.WAITER);
        String chat = link(person);
        telegram.configured = false;
        raise("D:off", person);

        assertThat(deliveryService.runOnce()).isZero();
        assertThat(deliveriesOf(person).get(0).getStatus()).isEqualTo("PENDING");

        telegram.configured = true;
        deliveryService.runOnce();
        assertThat(telegram.textsTo(chat)).hasSize(1);
    }
}
