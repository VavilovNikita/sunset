package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

/**
 * Ports lib/email.ts 1:1, including its fail-open contract: any failure here is caught and
 * logged, never propagated into the booking create/update flow that calls it.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final UserRepository userRepository;
    private final RestClient restClient;
    private final String resendApiKey;
    private final String from;
    private final String siteUrl;

    public EmailService(
            UserRepository userRepository,
            @Value("${app.email.resend-api-key}") String resendApiKey,
            @Value("${app.email.from}") String from,
            @Value("${app.email.site-url}") String siteUrl) {
        this.userRepository = userRepository;
        this.restClient = RestClient.builder().baseUrl("https://api.resend.com").build();
        this.resendApiKey = resendApiKey;
        this.from = from;
        this.siteUrl = siteUrl;
    }

    public void sendNewBookingEmail(BookingEntity booking, RoomEntity room) {
        try {
            // ADMIN/MANAGER only - now that WAITER/CASHIER exist (POS module), the full
            // userRepository.findAll() this used to send to would put room-booking
            // notifications in front of restaurant/bar staff who have nothing to do with them.
            List<String> to = userRepository.findByRoleIn(List.of(Role.ADMIN, Role.MANAGER)).stream()
                    .map(u -> u.getEmail())
                    .toList();
            if (to.isEmpty()) {
                return;
            }

            String html = "<p>New booking request received.</p>"
                    + "<ul>"
                    + "<li>Room: " + room.getName() + "</li>"
                    + "<li>Guest: " + booking.getGuestName() + " (" + booking.getGuestEmail() + ", " + booking.getGuestPhone() + ")</li>"
                    + "<li>Check-in: " + booking.getCheckIn() + "</li>"
                    + "<li>Check-out: " + booking.getCheckOut() + "</li>"
                    + "<li>Total: ฿" + formatThb(booking.getTotalPrice()) + "</li>"
                    + "</ul>"
                    + "<p><a href=\"" + siteUrl + "/admin/bookings/" + booking.getId() + "\">View in admin</a></p>";

            send(to, "New booking request — " + room.getName(), html);
        } catch (Exception e) {
            log.error("sendNewBookingEmail failed:", e);
        }
    }

    public void sendGuestStatusEmail(BookingEntity booking, RoomEntity room) {
        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.CANCELLED) {
            return;
        }
        try {
            String stay = room.getName() + " (" + booking.getCheckIn() + " → " + booking.getCheckOut() + ")";
            String subject = booking.getStatus() == BookingStatus.PAID
                    ? "Your booking is confirmed & paid — " + room.getName()
                    : "Your booking has been cancelled — " + room.getName();
            String html = booking.getStatus() == BookingStatus.PAID
                    ? "<p>Hi " + booking.getGuestName() + ",</p><p>Your payment for " + stay
                            + " has been received. We look forward to welcoming you!</p>"
                    : "<p>Hi " + booking.getGuestName() + ",</p><p>Your booking for " + stay
                            + " has been cancelled. If you have questions, just reply to this email.</p>";

            send(List.of(booking.getGuestEmail()), subject, html);
        } catch (Exception e) {
            log.error("sendGuestStatusEmail failed:", e);
        }
    }

    private void send(List<String> to, String subject, String html) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            // Deliberately does not log `to` or `html`: both can carry guest personal data
            // (email/phone/name/amount - see the callers above), and RESEND_API_KEY being blank
            // is the default in application.properties, not an exceptional state - any
            // environment where someone forgot to configure it would otherwise silently start
            // writing guest PII to the application log on every booking. Set RESEND_API_KEY to
            // send (and see) real email content.
            log.info("[email:dev-log] Would send \"{}\" to {} recipient(s) - RESEND_API_KEY not configured, email not sent.",
                    subject, to.size());
            return;
        }
        restClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + resendApiKey)
                .body(new ResendEmailRequest(from, to, subject, html))
                .retrieve()
                .toBodilessEntity();
    }

    private static String formatThb(BigDecimal totalPrice) {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMaximumFractionDigits(3);
        return format.format(totalPrice.doubleValue());
    }

    private record ResendEmailRequest(String from, List<String> to, String subject, String html) {
    }
}
