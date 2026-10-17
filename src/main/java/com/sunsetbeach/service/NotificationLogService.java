package com.sunsetbeach.service;

import com.sunsetbeach.entity.NotificationDeliveryEntity;
import com.sunsetbeach.entity.NotificationEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.NotificationDeliveryStatus;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.model.NotificationLogEntry;
import com.sunsetbeach.model.NotificationType;
import com.sunsetbeach.repository.NotificationDeliveryRepository;
import com.sunsetbeach.repository.NotificationRepository;
import com.sunsetbeach.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code GET /notifications/log}. The date filter is in hotel days, converted to the UTC the
 * {@code createdAt} column holds ({@link HotelDay}) - never {@code atStartOfDay()} on its own.
 */
@Service
public class NotificationLogService {

    static final int DEFAULT_LIMIT = 200;
    static final int MAX_LIMIT = 500;

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository deliveryRepository;
    private final UserRepository userRepository;
    private final Clock clock;

    public NotificationLogService(
            NotificationRepository notificationRepository, NotificationDeliveryRepository deliveryRepository, UserRepository userRepository, Clock clock) {
        this.notificationRepository = notificationRepository;
        this.deliveryRepository = deliveryRepository;
        this.userRepository = userRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<NotificationLogEntry> list(String from, String to, NotificationType type, NotificationDeliveryStatus status, Integer limit) {
        LocalDate fromDay = parseDay(from, "from");
        LocalDate toDay = parseDay(to, "to");
        if (fromDay != null && toDay != null && fromDay.isAfter(toDay)) {
            throw new BadRequestException("'from' must be on or before 'to'");
        }
        int size = limit == null ? DEFAULT_LIMIT : Math.max(1, Math.min(limit, MAX_LIMIT));

        Specification<NotificationEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (fromDay != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), HotelDay.startUtc(fromDay, clock.getZone())));
            }
            if (toDay != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), HotelDay.startUtc(toDay.plusDays(1), clock.getZone())));
            }
            if (type != null) {
                predicates.add(cb.equal(root.get("type"), type.getValue()));
            }
            if (status != null) {
                Subquery<String> sub = query.subquery(String.class);
                Root<NotificationDeliveryEntity> delivery = sub.from(NotificationDeliveryEntity.class);
                Predicate sameNotification = cb.equal(delivery.get("notificationId"), root.get("id"));
                if (status == NotificationDeliveryStatus.NO_CHANNEL) {
                    sub.select(delivery.get("id")).where(sameNotification);
                    predicates.add(cb.not(cb.exists(sub)));
                } else {
                    sub.select(delivery.get("id")).where(sameNotification, cb.equal(delivery.get("status"), status.getValue()));
                    predicates.add(cb.exists(sub));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<NotificationEntity> notifications =
                notificationRepository.findAll(spec, PageRequest.of(0, size, Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")))).getContent();
        if (notifications.isEmpty()) {
            return List.of();
        }
        Map<String, List<NotificationDeliveryEntity>> deliveries = deliveryRepository
                .findByNotificationIdIn(notifications.stream().map(NotificationEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(NotificationDeliveryEntity::getNotificationId));
        Map<String, UserEntity> users = userRepository.findAllById(notifications.stream().map(NotificationEntity::getRecipientUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        List<NotificationLogEntry> entries = new ArrayList<>();
        for (NotificationEntity n : notifications) {
            UserEntity recipient = users.get(n.getRecipientUserId());
            NotificationLogEntry entry = new NotificationLogEntry(
                    n.getId(),
                    NotificationType.fromValue(n.getType()),
                    n.getRecipientUserId(),
                    recipient == null ? "" : recipient.getName(),
                    NotificationLanguage.fromValue(n.getLanguage()),
                    n.getText(),
                    TimestampFormat.toUtc(n.getCreatedAt()),
                    NotificationDeliveryStatus.NO_CHANNEL,
                    0);
            List<NotificationDeliveryEntity> mine = deliveries.getOrDefault(n.getId(), List.of());
            if (!mine.isEmpty()) {
                NotificationDeliveryEntity shown = pick(mine);
                entry.deliveryStatus(NotificationDeliveryStatus.fromValue(shown.getStatus()));
                entry.attempts(shown.getAttempts());
                if ("PENDING".equals(shown.getStatus())) {
                    entry.sendAfter(TimestampFormat.toUtc(shown.getNextAttemptAt()));
                }
                if (shown.getSentAt() != null) {
                    entry.sentAt(TimestampFormat.toUtc(shown.getSentAt()));
                }
                if (shown.getLastError() != null) {
                    entry.error(shown.getLastError());
                }
            }
            entries.add(entry);
        }
        return entries;
    }

    /** With several channels one row would show the one that needs attention first: failed, then pending, then sent. */
    private static NotificationDeliveryEntity pick(List<NotificationDeliveryEntity> deliveries) {
        for (String wanted : new String[] {"FAILED", "PENDING"}) {
            for (NotificationDeliveryEntity d : deliveries) {
                if (wanted.equals(d.getStatus())) {
                    return d;
                }
            }
        }
        return deliveries.get(0);
    }

    private static LocalDate parseDay(String value, String name) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("'" + name + "' must be a date like 2026-10-17");
        }
    }
}
