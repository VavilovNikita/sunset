package com.sunsetbeach.service;

import com.sunsetbeach.entity.NotificationDeliveryEntity;
import com.sunsetbeach.entity.NotificationEntity;
import com.sunsetbeach.entity.StaffChannelLinkEntity;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.notification.TelegramClient;
import com.sunsetbeach.notification.TelegramClient.TelegramException;
import com.sunsetbeach.repository.NotificationDeliveryRepository;
import com.sunsetbeach.repository.NotificationRepository;
import com.sunsetbeach.repository.StaffChannelLinkRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Sends the {@code NotificationDelivery} rows that are due, with retries - the same shape as the
 * print queue ({@code PrintService#retryPendingJobs}): a plain {@code @Scheduled} fixed-delay sweep
 * over a small query, each item's failure caught and kept on its own row.
 *
 * <p>A delivery's failure never reaches the operation the notification belongs to: by the time a
 * delivery is attempted the notification is long written, in another transaction. A row records what
 * went wrong <em>in our own words</em> ({@code lastError}) - never Telegram's raw answer - and the
 * number of attempts. A retry waits for the end of quiet hours too. If Telegram says the recipient
 * blocked the bot, the link is dropped (a chat that blocked us will not come back by itself) and the
 * delivery fails for good.
 *
 * <p>Nothing is sent while the server has no bot token; the rows stay {@code PENDING} and go out
 * once it is configured.
 */
@Service
public class NotificationDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(NotificationDeliveryService.class);

    private static final int BATCH = 50;

    private final NotificationDeliveryRepository deliveryRepository;
    private final NotificationRepository notificationRepository;
    private final StaffChannelLinkRepository linkRepository;
    private final TelegramClient telegram;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;
    private final Clock clock;
    private final TransactionTemplate tx;
    private final int maxAttempts;

    public NotificationDeliveryService(
            NotificationDeliveryRepository deliveryRepository,
            NotificationRepository notificationRepository,
            StaffChannelLinkRepository linkRepository,
            TelegramClient telegram,
            NotificationService notificationService,
            AuditLogService auditLogService,
            Clock clock,
            PlatformTransactionManager transactionManager,
            @Value("${app.notifications.max-attempts:5}") int maxAttempts) {
        this.deliveryRepository = deliveryRepository;
        this.notificationRepository = notificationRepository;
        this.linkRepository = linkRepository;
        this.telegram = telegram;
        this.notificationService = notificationService;
        this.auditLogService = auditLogService;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
        this.maxAttempts = maxAttempts;
    }

    @Scheduled(fixedDelayString = "${app.notifications.delivery-interval-ms:5000}", initialDelayString = "${app.notifications.initial-delay-ms:10000}")
    public void sendDue() {
        try {
            runOnce();
        } catch (RuntimeException e) {
            log.error("Notification delivery sweep failed", e);
        }
    }

    /** One sweep; returns how many deliveries were handled (sent or failed an attempt). */
    public int runOnce() {
        if (!telegram.isConfigured()) {
            return 0;
        }
        LocalDateTime nowUtc = TimestampFormat.nowUtc(clock);
        List<NotificationDeliveryEntity> due = tx.execute(status -> deliveryRepository.findDue(nowUtc, PageRequest.of(0, BATCH)));
        int handled = 0;
        for (NotificationDeliveryEntity delivery : due == null ? List.<NotificationDeliveryEntity>of() : due) {
            try {
                if (deliverOne(delivery.getId())) {
                    handled++;
                }
            } catch (RuntimeException e) {
                log.error("Delivery {} failed unexpectedly", delivery.getId(), e);
            }
        }
        return handled;
    }

    private record Prepared(String userId, String chatId, String text) {
    }

    private boolean deliverOne(String deliveryId) {
        // 1. Read what to send (short transaction), 2. send with no transaction open, 3. record the result.
        Object prepared = tx.execute(status -> {
            NotificationDeliveryEntity d = deliveryRepository.findById(deliveryId).orElse(null);
            if (d == null || !"PENDING".equals(d.getStatus())) {
                return null;
            }
            NotificationEntity n = notificationRepository.findById(d.getNotificationId()).orElse(null);
            if (n == null) {
                return null;
            }
            if (!"TELEGRAM".equals(d.getChannel())) {
                fail(d, "This channel is not supported");
                return null;
            }
            Optional<StaffChannelLinkEntity> link = linkRepository.findByUserIdAndChannel(n.getRecipientUserId(), d.getChannel());
            if (link.isEmpty()) {
                fail(d, "No linked chat");
                return null;
            }
            return new Prepared(n.getRecipientUserId(), link.get().getExternalId(), n.getText());
        });
        if (!(prepared instanceof Prepared p)) {
            return false;
        }

        TelegramException failure = null;
        try {
            telegram.sendMessage(p.chatId(), p.text());
        } catch (TelegramException e) {
            failure = e;
        }

        TelegramException result = failure;
        tx.executeWithoutResult(status -> {
            NotificationDeliveryEntity d = deliveryRepository.findById(deliveryId).orElseThrow();
            LocalDateTime nowUtc = TimestampFormat.nowUtc(clock);
            d.setAttempts(d.getAttempts() + 1);
            if (result == null) {
                d.setStatus("SENT");
                d.setSentAt(nowUtc);
                d.setLastError(null);
            } else if (result.kind() == TelegramException.Kind.BLOCKED) {
                linkRepository.findByUserIdAndChannel(p.userId(), d.getChannel()).ifPresent(linkRepository::delete);
                auditLogService.recordSystemAction(
                        AuditAction.NOTIFICATION_CHANNEL_UNLINKED, AuditEntityType.USER, p.userId(), "Telegram link removed: the recipient blocked the bot");
                fail(d, "The recipient blocked the bot; the link was removed");
            } else if (result.kind() == TelegramException.Kind.REJECTED || d.getAttempts() >= maxAttempts) {
                fail(d, result.getMessage());
            } else {
                // 1, 2, 4, 8 minutes; and not into quiet hours.
                LocalDateTime next = nowUtc.plusMinutes(1L << (d.getAttempts() - 1));
                d.setLastError(result.getMessage());
                d.setNextAttemptAt(notificationService.deliverAfter(next));
            }
            deliveryRepository.save(d);
        });
        return true;
    }

    private void fail(NotificationDeliveryEntity d, String error) {
        d.setStatus("FAILED");
        d.setLastError(error);
        deliveryRepository.save(d);
    }
}
