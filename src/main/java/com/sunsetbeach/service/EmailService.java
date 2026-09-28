package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.RoomEntity;
import com.sunsetbeach.model.BookingStatus;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.repository.UserRepository;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

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
            // filter(Objects::nonNull): ADMIN/MANAGER accounts can now be created without a login
            // (see UserCreateInput) - an unlikely staffing shape for these two roles, but List.of
            // would throw building `to` the moment one exists, taking this notification down with
            // it. Email must never break the operation it accompanies (see CLAUDE.md).
            List<String> to = userRepository.findByRoleIn(List.of(Role.ADMIN, Role.MANAGER)).stream()
                    .map(u -> u.getEmail())
                    .filter(java.util.Objects::nonNull)
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

    /**
     * Staff-facing (never guest-facing) nudge for {@link BookingExpiryService}: one email per
     * sweep listing every unconfirmed public booking that's one business day away from being
     * auto-cancelled, not one email per booking. A per-booking email would turn the reminder
     * itself into an amplification channel for the exact flood this system defends against with
     * {@link com.sunsetbeach.security.BookingRateLimiter} and the auto-expiry sweep: N fake
     * bookings crossing the reminder threshold in the same 15-minute sweep would otherwise mean N
     * emails landing in ADMIN/MANAGER inboxes at once - the protection becoming the attack
     * surface. It also degrades gracefully on an ordinary busy weekend, where a dozen genuine
     * inquiries piling up should read as one clear list, not a dozen separate notifications.
     *
     * <p>Deliberately the same ADMIN/MANAGER audience as {@link #sendNewBookingEmail} - this is
     * the safety net meant to make the actual auto-cancellation a non-event in practice, not a
     * replacement for the guest-facing "cancelled" email {@link #sendGuestStatusEmail} sends for
     * a real, considered cancellation. Does nothing if {@code bookings} is empty - callers are
     * not required to check first.
     */
    public void sendBookingExpiringReminderDigestEmail(List<BookingEntity> bookings, Map<String, RoomEntity> roomsByRoomId) {
        if (bookings.isEmpty()) {
            return;
        }
        try {
            // See sendNewBookingEmail's own comment on the filter - same ADMIN/MANAGER audience,
            // same no-login-account possibility.
            List<String> to = userRepository.findByRoleIn(List.of(Role.ADMIN, Role.MANAGER)).stream()
                    .map(u -> u.getEmail())
                    .filter(java.util.Objects::nonNull)
                    .toList();
            if (to.isEmpty()) {
                return;
            }

            StringBuilder items = new StringBuilder();
            for (BookingEntity booking : bookings) {
                RoomEntity room = roomsByRoomId.get(booking.getRoomId());
                String roomName = room != null ? room.getName() : booking.getRoomId();
                items.append("<li>")
                        .append(roomName)
                        .append(" — ")
                        .append(booking.getGuestName())
                        .append(" (")
                        .append(booking.getGuestEmail())
                        .append(", ")
                        .append(booking.getGuestPhone())
                        .append("), ")
                        .append(booking.getCheckIn())
                        .append(" → ")
                        .append(booking.getCheckOut())
                        .append(" — <a href=\"")
                        .append(siteUrl)
                        .append("/admin/bookings/")
                        .append(booking.getId())
                        .append("\">review</a></li>");
            }

            String html = "<p>" + bookings.size() + " unconfirmed booking request(s) will be automatically cancelled "
                    + "after one more business day with no action:</p>"
                    + "<ul>" + items + "</ul>";

            send(to, bookings.size() + " unconfirmed booking request(s) expiring soon", html);
        } catch (Exception e) {
            log.error("sendBookingExpiringReminderDigestEmail failed:", e);
        }
    }

    public void sendGuestStatusEmail(BookingEntity booking, RoomEntity room) {
        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.CANCELLED) {
            return;
        }
        // A walk-in booking (POST /bookings/staff without contact info) legitimately has no
        // guestEmail - that's a normal, expected case, not a failure. Returning here avoids both
        // an NPE (List.of(null) throws) and, more importantly, an ERROR-level log entry that
        // would misleadingly look like a real send failure every single time a walk-in's booking
        // is marked PAID/CANCELLED.
        if (booking.getGuestEmail() == null) {
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

    /**
     * Sent by {@code GuestAccountService} on register/resend - same fail-open contract as every
     * other method here (the account is still created/regenerated even if this fails; the
     * {@code POST /guest-auth/register}/{@code resend-verification} response is identical either
     * way, see those operations' own descriptions). {@code name} is nullable (optional at
     * register) and only used for the greeting, never for lookup.
     */
    public void sendGuestVerificationEmail(String email, String name, String token) {
        try {
            String greetingName = (name == null || name.isBlank()) ? "there" : name;
            String link = siteUrl + "/guest/verify?token=" + token;
            String html = "<p>Hi " + greetingName + ",</p>"
                    + "<p>Verify your email to activate your guest account:</p>"
                    + "<p><a href=\"" + link + "\">Verify email</a></p>"
                    + "<p>This link expires in 24 hours. If you didn't request this, you can ignore this email.</p>";

            send(List.of(email), "Verify your email — The Sunset Beach Resort & Spa", html);
        } catch (Exception e) {
            log.error("sendGuestVerificationEmail failed:", e);
        }
    }

    public static final String PRE_ARRIVAL_SUBJECT = "Your stay is coming up — The Sunset Beach Resort & Spa";
    public static final String POST_STAY_SUBJECT = "Thank you for staying with us — The Sunset Beach Resort & Spa";
    public static final String WIN_BACK_SUBJECT = "We'd love to welcome you back — The Sunset Beach Resort & Spa";

    /**
     * The three lifecycle emails {@link LifecycleEmailService} sends. Unlike every method above,
     * these are not something the guest just caused, so each carries an unsubscribe link (in the
     * body and as a {@code List-Unsubscribe} header) - see that class's javadoc. Same fail-open
     * contract, but each returns whether the email actually went out: the sweep logs a
     * {@code GuestEmailLog} row only on {@code true}, so the log (and the guest card's email
     * history) is what was sent, not what was attempted. An unconfigured {@code RESEND_API_KEY}
     * returns {@code false} for the same reason - nothing went out.
     *
     * <p>{@code name} is the guest's own free text - escaped here, unlike the staff-typed values
     * the older methods above interpolate as-is.
     */
    public boolean sendPreArrivalEmail(String email, String name, String roomName, LocalDate checkIn, LocalDate checkOut, String unsubscribeToken) {
        try {
            String html = "<p>Hi " + escapeName(name) + ",</p>"
                    + "<p>We're looking forward to welcoming you on " + checkIn + ".</p>"
                    + "<ul>"
                    + "<li>Room: " + HtmlUtils.htmlEscape(roomName) + "</li>"
                    + "<li>Check-in: " + checkIn + "</li>"
                    + "<li>Check-out: " + checkOut + "</li>"
                    + "</ul>"
                    + "<p>If anything has changed, or there's something we can arrange before you arrive, just reply to this email.</p>"
                    + unsubscribeFooter(unsubscribeToken);
            return sendLifecycle(email, PRE_ARRIVAL_SUBJECT, html, unsubscribeToken);
        } catch (Exception e) {
            log.error("sendPreArrivalEmail failed:", e);
            return false;
        }
    }

    /** {@code reviewUrl} null or blank leaves the review section out - see {@code LifecycleEmailSettings}. */
    public boolean sendPostStayEmail(String email, String name, String roomName, LocalDate checkIn, LocalDate checkOut, String reviewUrl, String unsubscribeToken) {
        try {
            String review = reviewUrl == null || reviewUrl.isBlank()
                    ? ""
                    : "<p>If you have a moment, we'd be grateful for a review:</p>"
                            + "<p><a href=\"" + HtmlUtils.htmlEscape(reviewUrl) + "\">Leave a review</a></p>";
            String html = "<p>Hi " + escapeName(name) + ",</p>"
                    + "<p>Thank you for staying with us in " + HtmlUtils.htmlEscape(roomName) + " (" + checkIn + " → " + checkOut
                    + "). We hope you had a wonderful time.</p>"
                    + review
                    + "<p>If there's anything you'd like to tell us about your stay, just reply to this email.</p>"
                    + unsubscribeFooter(unsubscribeToken);
            return sendLifecycle(email, POST_STAY_SUBJECT, html, unsubscribeToken);
        } catch (Exception e) {
            log.error("sendPostStayEmail failed:", e);
            return false;
        }
    }

    public boolean sendWinBackEmail(String email, String name, String unsubscribeToken) {
        try {
            String html = "<p>Hi " + escapeName(name) + ",</p>"
                    + "<p>It's been a while since your last stay with us, and we'd love to welcome you back to Koh Samui.</p>"
                    + "<p><a href=\"" + siteUrl + "/booking\">See our rooms and availability</a></p>"
                    + unsubscribeFooter(unsubscribeToken);
            return sendLifecycle(email, WIN_BACK_SUBJECT, html, unsubscribeToken);
        } catch (Exception e) {
            log.error("sendWinBackEmail failed:", e);
            return false;
        }
    }

    private String unsubscribeUrl(String unsubscribeToken) {
        return siteUrl + "/guest/unsubscribe?token=" + URLEncoder.encode(unsubscribeToken, StandardCharsets.UTF_8);
    }

    private String unsubscribeFooter(String unsubscribeToken) {
        return "<p style=\"font-size:12px;color:#888\">You're receiving this because you have a guest account with us. "
                + "<a href=\"" + unsubscribeUrl(unsubscribeToken) + "\">Unsubscribe</a> from these emails.</p>";
    }

    private static String escapeName(String name) {
        return name == null || name.isBlank() ? "there" : HtmlUtils.htmlEscape(name);
    }

    /** Returns false (nothing sent) when RESEND_API_KEY isn't configured - see {@link #send}. */
    private boolean sendLifecycle(String to, String subject, String html, String unsubscribeToken) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            send(List.of(to), subject, html);
            return false;
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("from", from);
        body.put("to", List.of(to));
        body.put("subject", subject);
        body.put("html", html);
        // Mail clients show their own "Unsubscribe" control from this - it opens the same page the
        // body's link does. No List-Unsubscribe-Post (RFC 8058 one-click): that needs a POST
        // endpoint mail providers call directly, and GET /guest-auth/unsubscribe is behind the
        // site's own page on purpose (see its openapi.yaml description).
        body.put("headers", Map.of("List-Unsubscribe", "<" + unsubscribeUrl(unsubscribeToken) + ">"));
        restClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer " + resendApiKey)
                .body(body)
                .retrieve()
                .toBodilessEntity();
        return true;
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
