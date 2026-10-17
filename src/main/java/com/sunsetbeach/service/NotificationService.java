package com.sunsetbeach.service;

import com.sunsetbeach.entity.NotificationDeliveryEntity;
import com.sunsetbeach.entity.NotificationSettingsEntity;
import com.sunsetbeach.entity.StaffChannelLinkEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.model.NotificationType;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.QuietHours;
import com.sunsetbeach.repository.NotificationDeliveryRepository;
import com.sunsetbeach.repository.NotificationRepository;
import com.sunsetbeach.repository.StaffChannelLinkRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * The channel-independent core of the notification centre: {@link #raise} writes one
 * {@code Notification} per recipient, already worded in that person's language, and one
 * {@code NotificationDelivery} per channel the person has linked. Nothing here knows about
 * Telegram - {@link NotificationDeliveryService} is the part that talks to a channel, and a LINE or
 * WhatsApp adapter would plug in next to it.
 *
 * <p><b>Idempotent by the database.</b> {@code (dedupKey, recipient)} is a unique index and the
 * insert is {@code ON CONFLICT DO NOTHING}, so a scanner pass that finds the same condition again,
 * or two passes racing, raise it once - not a check in code that two threads can both pass.
 *
 * <p><b>Quiet hours hold, they don't drop.</b> A message raised inside them is written at once (it
 * shows in the log) but its delivery is not due until they end. Each message waits on its own row;
 * nothing is merged into a digest.
 *
 * <p>Callers run this inside their own transaction and wrap it in their own try/catch: a
 * notification must never break the operation it accompanies. A failure inside the SQL would still
 * poison a caller's transaction, which is why the callers that sit inside a business operation
 * (block creation) raise only after it has committed - see {@link NotificationEvents}.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    static final String CHANNEL_TELEGRAM = "TELEGRAM";

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final StaffChannelLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final NotificationSettingsService settingsService;
    private final Clock clock;
    private final String siteUrl;

    public NotificationService(
            NotificationRepository notificationRepository,
            NotificationDeliveryRepository deliveryRepository,
            StaffChannelLinkRepository linkRepository,
            UserRepository userRepository,
            NotificationSettingsService settingsService,
            Clock clock,
            @Value("${app.email.site-url:}") String siteUrl) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.settingsService = settingsService;
        this.clock = clock;
        this.siteUrl = siteUrl == null ? "" : siteUrl.trim().replaceAll("/+$", "");
    }

    /** Param key a caller sets to an admin path (e.g. {@code /admin/maintenance}); becomes {@code {link}} when a site URL is configured. */
    public static final String PATH = "path";

    /**
     * @return how many recipients were newly notified (an already-raised {@code dedupKey} counts 0).
     *         Recipients that are unknown or deactivated are skipped.
     */
    @Transactional
    public int raise(
            NotificationType type,
            NotificationMessages.Key key,
            java.util.Collection<String> recipientUserIds,
            Map<String, String> params,
            String dedupKey) {
        NotificationSettingsEntity settings = settingsService.getEntity();
        NotificationLanguage defaultLanguage = NotificationLanguage.fromValue(settings.getDefaultLanguage());
        LocalDateTime nowUtc = TimestampFormat.nowUtc(clock);
        LocalDateTime deliverAt = deliverAfter(settings, nowUtc);

        Map<String, String> filled = new HashMap<>(params);
        String path = filled.remove(PATH);
        filled.put("link", path != null && !siteUrl.isEmpty() ? siteUrl + path : "");

        int raised = 0;
        for (String userId : new LinkedHashSet<>(recipientUserIds)) {
            UserEntity user = userRepository.findById(userId).orElse(null);
            if (user == null || !user.isActive()) {
                continue;
            }
            NotificationLanguage language =
                    user.getNotificationLanguage() != null ? NotificationLanguage.fromValue(user.getNotificationLanguage()) : defaultLanguage;
            Map<String, String> forRecipient = new HashMap<>(filled);
            if (forRecipient.containsKey("function")) {
                forRecipient.put("function", NotificationMessages.functionName(forRecipient.get("function"), language));
            }
            String text = NotificationMessages.render(key, language, forRecipient);
            String id = UUID.randomUUID().toString();
            int inserted = notificationRepository.insertIfAbsent(id, type.getValue(), key.name(), userId, language.getValue(), text, dedupKey, nowUtc);
            if (inserted == 0) {
                continue;
            }
            raised++;
            List<StaffChannelLinkEntity> links = linkRepository.findByUserId(userId);
            for (StaffChannelLinkEntity link : links) {
                NotificationDeliveryEntity delivery = new NotificationDeliveryEntity();
                delivery.setNotificationId(id);
                delivery.setChannel(link.getChannel());
                delivery.setStatus("PENDING");
                delivery.setNextAttemptAt(deliverAt);
                deliveryRepository.save(delivery);
            }
        }
        if (raised > 0) {
            log.info("Raised {} {} notification(s) ({})", raised, type.getValue(), dedupKey);
        }
        return raised;
    }

    /** UTC moment from which something raised now may be sent: now, or the end of quiet hours (hotel time) converted to UTC. */
    LocalDateTime deliverAfter(NotificationSettingsEntity settings, LocalDateTime nowUtc) {
        if (!settings.isQuietHoursEnabled()) {
            return nowUtc;
        }
        LocalDateTime hotelNow = nowUtc.atOffset(ZoneOffset.UTC).atZoneSameInstant(clock.getZone()).toLocalDateTime();
        return QuietHours.endOfQuietHours(hotelNow, settings.getQuietStart(), settings.getQuietEnd())
                .map(end -> end.atZone(clock.getZone()).withZoneSameInstant(ZoneOffset.UTC).toLocalDateTime())
                .orElse(nowUtc);
    }

    /** Same rule for a retry: a delivery retried at 22:30 also waits for the morning. */
    LocalDateTime deliverAfter(LocalDateTime notBeforeUtc) {
        return deliverAfter(settingsService.getEntity(), notBeforeUtc);
    }

    /** A distinct list helper for callers building recipient lists. */
    public static List<String> distinct(java.util.Collection<String> ids) {
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
