package com.sunsetbeach.controller;

import com.sunsetbeach.api.NotificationsApi;
import com.sunsetbeach.model.NotificationChannel;
import com.sunsetbeach.model.NotificationDeliveryStatus;
import com.sunsetbeach.model.NotificationLanguageInput;
import com.sunsetbeach.model.NotificationLinkCode;
import com.sunsetbeach.model.NotificationLinkCodeInput;
import com.sunsetbeach.model.NotificationLogEntry;
import com.sunsetbeach.model.NotificationStaffMember;
import com.sunsetbeach.model.NotificationStatus;
import com.sunsetbeach.model.NotificationType;
import com.sunsetbeach.service.NotificationChannelService;
import com.sunsetbeach.service.NotificationLogService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NotificationController implements NotificationsApi {

    private final NotificationChannelService channelService;
    private final NotificationLogService logService;

    public NotificationController(NotificationChannelService channelService, NotificationLogService logService) {
        this.channelService = channelService;
        this.logService = logService;
    }

    @Override
    public ResponseEntity<NotificationStatus> getNotificationStatus() {
        return ResponseEntity.ok(channelService.status());
    }

    @Override
    public ResponseEntity<List<NotificationStaffMember>> listNotificationStaff() {
        return ResponseEntity.ok(channelService.listStaff());
    }

    @Override
    public ResponseEntity<NotificationStaffMember> setNotificationStaffLanguage(String userId, NotificationLanguageInput input) {
        return ResponseEntity.ok(channelService.setLanguage(userId, input.getLanguage()));
    }

    @Override
    public ResponseEntity<NotificationLinkCode> createNotificationLinkCode(NotificationLinkCodeInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(channelService.createLinkCode(input.getUserId(), NotificationChannel.TELEGRAM));
    }

    @Override
    public ResponseEntity<Void> deleteNotificationLink(String userId, NotificationChannel channel) {
        channelService.unlink(userId, channel);
        return ResponseEntity.noContent().build();
    }

    @Override
    public ResponseEntity<List<NotificationLogEntry>> listNotificationLog(
            String from, String to, NotificationType type, NotificationDeliveryStatus deliveryStatus, Integer limit) {
        return ResponseEntity.ok(logService.list(from, to, type, deliveryStatus, limit));
    }
}
