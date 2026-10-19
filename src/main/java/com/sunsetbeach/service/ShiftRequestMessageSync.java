package com.sunsetbeach.service;

import com.sunsetbeach.entity.NotificationDeliveryEntity;
import com.sunsetbeach.entity.NotificationEntity;
import com.sunsetbeach.entity.ShiftChangeRequestEntity;
import com.sunsetbeach.entity.StaffChannelLinkEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.notification.TelegramClient;
import com.sunsetbeach.repository.NotificationDeliveryRepository;
import com.sunsetbeach.repository.NotificationRepository;
import com.sunsetbeach.repository.StaffChannelLinkRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Once a shift request is settled, every message that asked someone to decide it is rewritten to the
 * outcome ("Approved by Anna at 14:05") with its buttons removed, so a second decider does not press in
 * vain. Runs after the request's transaction has committed and never throws - an unreachable Telegram
 * leaves the old buttons, which still answer correctly with the request's current status when pressed.
 */
@Service
public class ShiftRequestMessageSync {

    private static final Logger log = LoggerFactory.getLogger(ShiftRequestMessageSync.class);
    private static final DateTimeFormatter HHMM = DateTimeFormatter.ofPattern("HH:mm");

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final StaffChannelLinkRepository linkRepository;
    private final UserRepository userRepository;
    private final TelegramClient telegram;
    private final Clock clock;

    public ShiftRequestMessageSync(
            NotificationRepository notificationRepository,
            NotificationDeliveryRepository deliveryRepository,
            StaffChannelLinkRepository linkRepository,
            UserRepository userRepository,
            TelegramClient telegram,
            Clock clock) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.linkRepository = linkRepository;
        this.userRepository = userRepository;
        this.telegram = telegram;
        this.clock = clock;
    }

    /** The one-line outcome of a settled request in {@code language}. */
    public String outcomeLine(ShiftChangeRequestEntity request, NotificationLanguage language) {
        Map<String, String> params = new HashMap<>();
        params.put("time", request.getDecidedAt() == null ? "" : hotelTime(request));
        String by = request.getDecidedByUserId() == null ? "" : userRepository.findById(request.getDecidedByUserId()).map(UserEntity::getName).orElse("");
        params.put("by", by);
        Key key = switch (request.getStatus()) {
            case "APPROVED" -> Key.OUTCOME_APPROVED;
            case "REJECTED" -> Key.OUTCOME_REJECTED;
            case "CANCELLED" -> Key.OUTCOME_CANCELLED;
            default -> Key.OUTCOME_EXPIRED;
        };
        if (key == Key.OUTCOME_EXPIRED) {
            ShiftRequestExpiry why = ShiftRequestExpiry.of(request.getExpiredReason());
            params.put("why", why == null ? "" : why.text(language));
        }
        return NotificationMessages.render(key, language, params);
    }

    private String hotelTime(ShiftChangeRequestEntity request) {
        LocalTime time = request.getDecidedAt().atOffset(ZoneOffset.UTC).atZoneSameInstant(clock.getZone()).toLocalTime();
        return time.format(HHMM);
    }

    /** Rewrites every sent message about {@code request} to its outcome. */
    public void settled(ShiftChangeRequestEntity request) {
        try {
            List<NotificationEntity> notifications = new ArrayList<>(notificationRepository.findByDedupKeyStartingWith("SHIFTREQ_NEW:" + request.getId()));
            notifications.addAll(notificationRepository.findByDedupKeyStartingWith("SHIFTREQ_REMIND:" + request.getId() + ":"));
            if (notifications.isEmpty()) {
                return;
            }
            Map<String, NotificationEntity> byId = new HashMap<>();
            notifications.forEach(n -> byId.put(n.getId(), n));
            for (NotificationDeliveryEntity delivery : deliveryRepository.findByNotificationIdIn(byId.keySet())) {
                if (!"SENT".equals(delivery.getStatus()) || delivery.getExternalMessageId() == null || !"TELEGRAM".equals(delivery.getChannel())) {
                    continue;
                }
                NotificationEntity notification = byId.get(delivery.getNotificationId());
                StaffChannelLinkEntity link = linkRepository.findByUserIdAndChannel(notification.getRecipientUserId(), "TELEGRAM").orElse(null);
                if (link == null) {
                    continue;
                }
                String text = notification.getText() + "\n\n" + outcomeLine(request, NotificationLanguage.fromValue(notification.getLanguage()));
                try {
                    telegram.editMessageText(link.getExternalId(), delivery.getExternalMessageId(), text, null);
                } catch (TelegramClient.TelegramException e) {
                    log.warn("Could not update a request message: {}", e.getMessage());
                }
            }
        } catch (RuntimeException e) {
            log.error("Updating the messages of shift request {} failed", request.getId(), e);
        }
    }
}
