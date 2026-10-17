package com.sunsetbeach.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sunsetbeach.entity.ChannelLinkCodeEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.model.NotificationChannel;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.model.NotificationLinkCode;
import com.sunsetbeach.model.NotificationSettings;
import com.sunsetbeach.model.NotificationSettingsUpdateInput;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.notification.TelegramClient.TelegramUpdate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Linking a Telegram chat without a login: single-use, 15-minute codes, one chat per employee and one
 * employee per chat, {@code /stop}, a deactivated employee, and the settings the form saves.
 */
class NotificationChannelTests extends NotificationTestBase {

    private static final Pattern CODE_SHAPE = Pattern.compile("^[A-Za-z0-9_-]{1,64}$");

    @Autowired private UserService userService;

    private UserEntity manager;
    private UserEntity somchai;

    @BeforeEach
    void people() {
        manager = user("Manager", Role.MANAGER);
        somchai = user("Somchai", Role.WAITER);
        actAs(manager);
    }

    private static String codeOf(NotificationLinkCode link) {
        return link.getUrl().substring(link.getUrl().indexOf("?start=") + "?start=".length());
    }

    private void say(String chat, String text) {
        telegram.incoming.add(new TelegramUpdate(nextUpdateId++, chat, text));
        pollService.pollOnce();
    }

    private long nextUpdateId = 1000;

    @Test
    void theLinkIsATelegramDeepLinkWithAnUnguessableCodeOfTheAllowedShape() {
        NotificationLinkCode link = channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM);

        assertThat(link.getUrl()).startsWith("https://t.me/sunset_test_bot?start=");
        assertThat(codeOf(link)).matches(CODE_SHAPE);
        assertThat(codeOf(link).length()).isBetween(20, 64);
        assertThat(link.getExpiresAt().toLocalDateTime()).isEqualTo(LocalDateTime.parse(DAY + "T03:15")); // 10:00 Bangkok + 15 minutes, in UTC

        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 40; i++) {
            String code = codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM));
            assertThat(code).matches(CODE_SHAPE);
            assertThat(seen.add(code)).isTrue();
        }
    }

    @Test
    void startWithTheCode_linksTheChat_andAnswersInTheEmployeesLanguage() {
        channelService.setLanguage(somchai.getId(), NotificationLanguage.RU);
        String code = codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM));

        say("chat-1", "/start " + code);

        assertThat(linkRepository.findByUserIdAndChannel(somchai.getId(), "TELEGRAM")).get().extracting("externalId").isEqualTo("chat-1");
        assertThat(telegram.textsTo("chat-1")).containsExactly("Привязано: Somchai");
        assertThat(channelService.listStaff()).filteredOn(m -> m.getUserId().equals(somchai.getId())).first().satisfies(m -> {
            assertThat(m.getTelegramLinked()).isTrue();
            assertThat(m.getLanguage()).isEqualTo(NotificationLanguage.RU);
        });
    }

    @Test
    void aCodeWorksOnce() {
        String code = codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM));
        say("chat-1", "/start " + code);

        say("chat-2", "/start " + code);

        assertThat(telegram.textsTo("chat-2")).hasSize(1).first().asString().contains("invalid or has expired");
        assertThat(linkRepository.findByChannelAndExternalId("TELEGRAM", "chat-2")).isEmpty();
        assertThat(linkRepository.findByUserIdAndChannel(somchai.getId(), "TELEGRAM")).get().extracting("externalId").isEqualTo("chat-1");
    }

    @Test
    void anExpiredCode_linksNothing() {
        String code = codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM));

        clock.set(DAY + "T10:15:01");
        say("chat-1", "/start " + code);

        assertThat(linkRepository.findByUserIdAndChannel(somchai.getId(), "TELEGRAM")).isEmpty();
        assertThat(telegram.textsTo("chat-1")).first().asString().contains("invalid or has expired");
    }

    @Test
    void aCodeIsStillGoodAtTheLastSecond() {
        String code = codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM));

        clock.set(DAY + "T10:14:59");
        say("chat-1", "/start " + code);

        assertThat(linkRepository.findByUserIdAndChannel(somchai.getId(), "TELEGRAM")).isPresent();
    }

    @Test
    void aNewCode_replacesTheOldOne() {
        String first = codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM));
        String second = codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM));

        say("chat-1", "/start " + first);
        assertThat(linkRepository.findByUserIdAndChannel(somchai.getId(), "TELEGRAM")).isEmpty();

        say("chat-1", "/start " + second);
        assertThat(linkRepository.findByUserIdAndChannel(somchai.getId(), "TELEGRAM")).isPresent();
    }

    @Test
    void garbageInPlaceOfACode_isRefusedWithoutTouchingTheDatabase() {
        say("chat-1", "/start ../../etc/passwd");
        say("chat-1", "/start " + "a".repeat(65));
        say("chat-1", "/start no-such-code");

        assertThat(telegram.textsTo("chat-1")).hasSize(3).allSatisfy(t -> assertThat(t).contains("invalid or has expired"));
        assertThat(linkRepository.findByChannelAndExternalId("TELEGRAM", "chat-1")).isEmpty();
    }

    @Test
    void aChatAlreadyLinkedToSomeoneElse_cannotTakeAnother_andTheCodeSurvives() {
        UserEntity pim = user("Pim", Role.WAITER);
        say("chat-1", "/start " + codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM)));
        String pimsCode = codeOf(channelService.createLinkCode(pim.getId(), NotificationChannel.TELEGRAM));

        say("chat-1", "/start " + pimsCode);

        assertThat(telegram.textsTo("chat-1")).last().asString().contains("already linked to another employee");
        assertThat(linkRepository.findByUserIdAndChannel(pim.getId(), "TELEGRAM")).isEmpty();
        assertThat(linkRepository.findByUserIdAndChannel(somchai.getId(), "TELEGRAM")).isPresent();

        say("chat-2", "/start " + pimsCode); // the rejected attempt did not spend it
        assertThat(linkRepository.findByUserIdAndChannel(pim.getId(), "TELEGRAM")).get().extracting("externalId").isEqualTo("chat-2");
    }

    @Test
    void relinkingFromAnotherChat_movesTheEmployee() {
        say("chat-1", "/start " + codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM)));

        say("chat-2", "/start " + codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM)));

        assertThat(linkRepository.findByUserIdAndChannel(somchai.getId(), "TELEGRAM")).get().extracting("externalId").isEqualTo("chat-2");
        assertThat(linkRepository.findByChannelAndExternalId("TELEGRAM", "chat-1")).isEmpty();
    }

    @Test
    void twoChatsOpeningTheSameLinkAtOnce_endWithExactlyOneLink() throws Exception {
        String code = codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM));
        int threads = 6;
        CyclicBarrier barrier = new CyclicBarrier(threads);
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        try {
            List<Future<?>> futures = new ArrayList<>();
            for (int i = 0; i < threads; i++) {
                String chat = "race-chat-" + i;
                futures.add(pool.submit(() -> {
                    barrier.await();
                    channelService.handleUpdate(new TelegramUpdate(1, chat, "/start " + code));
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(linkRepository.findByUserId(somchai.getId())).hasSize(1);
        long linkedReplies = telegram.sent.stream().filter(s -> s.text().startsWith("Linked:")).count();
        assertThat(linkedReplies).isEqualTo(1);
    }

    @Test
    void stop_unlinksTheChat_andAnswers() {
        say("chat-1", "/start " + codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM)));

        say("chat-1", "/stop");

        assertThat(linkRepository.findByUserIdAndChannel(somchai.getId(), "TELEGRAM")).isEmpty();
        assertThat(telegram.textsTo("chat-1")).last().asString().contains("Unlinked");

        say("chat-1", "/stop");
        assertThat(telegram.textsTo("chat-1")).last().asString().contains("not linked");
    }

    @Test
    void managerUnlink_removesTheLink_and404sWhenThereIsNone() {
        say("chat-1", "/start " + codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM)));

        channelService.unlink(somchai.getId(), NotificationChannel.TELEGRAM);

        assertThat(linkRepository.findByUserIdAndChannel(somchai.getId(), "TELEGRAM")).isEmpty();
        assertThatThrownBy(() -> channelService.unlink(somchai.getId(), NotificationChannel.TELEGRAM)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void deactivatingAnEmployee_dropsTheirLink() {
        say("chat-1", "/start " + codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM)));

        userService.setActive(somchai.getId(), manager.getId(), false);

        assertThat(linkRepository.findByUserId(somchai.getId())).isEmpty();
        assertThatThrownBy(() -> channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM)).isInstanceOf(ConflictException.class);
    }

    @Test
    void linkingNeedsAConfiguredBot_andAKnownUser() {
        telegram.configured = false;
        assertThatThrownBy(() -> channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("not configured");
        telegram.configured = true;
        assertThatThrownBy(() -> channelService.createLinkCode("no-such-user", NotificationChannel.TELEGRAM)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void statusComesFromConfigurationAndTheCachedBotName_withoutAskingTelegram() {
        telegram.configured = false;
        assertThat(channelService.status().getTelegramConfigured()).isFalse();
        assertThat(channelService.status().getBotUsername().isPresent()).isFalse();

        telegram.configured = true;
        channelService.refreshBotUsername();
        telegram.botUsername = "renamed_bot"; // a later change at Telegram is not seen by a status read
        assertThat(channelService.status().getTelegramConfigured()).isTrue();
        assertThat(channelService.status().getBotUsername().get()).isEqualTo("sunset_test_bot");
    }

    @Test
    void thePollLearnsTheBotNameFromGetMeWhenItIsNotKnownYet() {
        assertThat(channelService.status().getBotUsername().isPresent()).isFalse();

        pollService.pollOnce();

        assertThat(channelService.status().getBotUsername().get()).isEqualTo("sunset_test_bot");
    }

    @Test
    void theUpdateOffsetIsKeptInTheDatabase_soNothingIsReplayed() {
        telegram.incoming.add(new TelegramUpdate(5000, "chat-1", "hello"));
        telegram.incoming.add(new TelegramUpdate(5001, "chat-1", "hello again"));
        pollService.pollOnce();

        assertThat(botStateRepository.findById(1).orElseThrow().getUpdateOffset()).isEqualTo(5002);
        pollService.pollOnce();
        assertThat(telegram.lastRequestedOffset).isEqualTo(5002);
    }

    @Test
    void aBadMessage_doesNotStopTheQueue() {
        telegram.incoming.add(new TelegramUpdate(6000, null, null)); // not a private text message
        telegram.incoming.add(new TelegramUpdate(6001, "chat-1", "/start " + codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM))));
        pollService.pollOnce();

        assertThat(linkRepository.findByUserIdAndChannel(somchai.getId(), "TELEGRAM")).isPresent();
    }

    @Test
    void withoutAToken_theBotIsNotPolled() {
        telegram.configured = false;
        telegram.incoming.add(new TelegramUpdate(7000, "chat-1", "hi"));

        assertThat(pollService.pollOnce()).isZero();
        assertThat(telegram.lastRequestedOffset).isEqualTo(-1);
    }

    // ---- the settings the form saves ------------------------------------------------------------------

    private NotificationSettingsUpdateInput validInput() {
        NotificationSettings current = settingsService.get();
        return new NotificationSettingsUpdateInput(
                current.getLateEnabled(), settingsService.minLateGraceMinutes(), current.getAbsenceEnabled(), 120, true, true, true, true, 15, true, "22:00", "07:00", "09:00",
                NotificationLanguage.EN, List.of(), List.of());
    }

    @Test
    void theGraceCannotBeShorterThanTheTerminalsPollInterval() {
        int min = settingsService.minLateGraceMinutes();
        NotificationSettingsUpdateInput tooShort = validInput().lateGraceMinutes(min - 1 == 0 ? 1 : min - 1);
        // In this test context the terminal poll interval is an hour, so the floor is 60 minutes.
        assertThat(min).isEqualTo(60);

        assertThatThrownBy(() -> settingsService.update(tooShort)).isInstanceOf(ValidationException.class);
        assertThat(settingsService.update(validInput().lateGraceMinutes(min)).getLateGraceMinutes()).isEqualTo(min);
    }

    @Test
    void theProductionFloorIsTheFiveMinuteTerminalPoll() {
        NotificationSettingsService production = new NotificationSettingsService(null, null, null, 300_000);
        assertThat(production.minLateGraceMinutes()).isEqualTo(5);
        assertThat(new NotificationSettingsService(null, null, null, 301_000).minLateGraceMinutes()).isEqualTo(6);
    }

    @Test
    void absenceCannotBeShorterThanTheGrace_andRecipientsMustBeActiveUsers() {
        int min = settingsService.minLateGraceMinutes();
        assertThatThrownBy(() -> settingsService.update(validInput().lateGraceMinutes(min + 30).absenceMinutes(min + 10))).isInstanceOf(ValidationException.class);

        UserEntity gone = user("Gone", Role.WAITER);
        gone.setActive(false);
        userRepository.saveAndFlush(gone);
        assertThatThrownBy(() -> settingsService.update(validInput().lateRecipientUserIds(List.of(gone.getId())))).isInstanceOf(ValidationException.class);
        assertThatThrownBy(() -> settingsService.update(validInput().directorUserIds(List.of("nobody")))).isInstanceOf(ValidationException.class);
    }

    @Test
    void quietHoursThatStartAndEndTogether_areRefusedUnlessOff() {
        assertThatThrownBy(() -> settingsService.update(validInput().quietStart("22:00").quietEnd("22:00"))).isInstanceOf(ValidationException.class);
        assertThat(settingsService.update(validInput().quietHoursEnabled(false).quietStart("22:00").quietEnd("22:00")).getQuietHoursEnabled()).isFalse();
    }

    @Test
    void savingSettings_isAudited_andAnUnchangedSaveWritesNothing() {
        UserEntity director = user("Director", Role.ADMIN);
        settingsService.update(validInput().directorUserIds(List.of(director.getId())));
        long before = auditCount();

        settingsService.update(validInput().directorUserIds(List.of(director.getId()))); // identical
        assertThat(auditCount()).isEqualTo(before);

        settingsService.update(validInput().directorUserIds(List.of(director.getId())).absenceMinutes(180));
        assertThat(auditCount()).isEqualTo(before + 1);
        assertThat(auditLogRepository.findAll().stream().filter(a -> a.getSummary().startsWith("Notification settings:") && a.getSummary().contains("absence after 120 min -> 180 min")))
                .isNotEmpty();
    }

    private long auditCount() {
        return auditLogRepository.findAll().stream().filter(a -> a.getSummary().startsWith("Notification settings:")).count();
    }

    @Test
    void theLinkCodeIsNeverWrittenToTheAuditLog() {
        String code = codeOf(channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM));

        assertThat(auditLogRepository.findAll().stream().filter(a -> a.getSummary().contains(code))).isEmpty();
        assertThat(auditLogRepository.findAll().stream().filter(a -> userIds.contains(a.getEntityId() == null ? "" : a.getEntityId()) && a.getSummary().contains("link code created")))
                .isNotEmpty();
    }

    @Test
    void expiredCodesAreSweptWhenANewOneIsMade() {
        ChannelLinkCodeEntity stale = new ChannelLinkCodeEntity();
        stale.setCode("stale-code-for-test");
        stale.setUserId(somchai.getId());
        stale.setChannel("TELEGRAM");
        stale.setExpiresAt(LocalDateTime.parse("2033-05-10T00:00"));
        codeRepository.saveAndFlush(stale);

        channelService.createLinkCode(somchai.getId(), NotificationChannel.TELEGRAM);

        assertThat(codeRepository.findById("stale-code-for-test")).isEmpty();
    }
}
