package com.sunsetbeach.service;

import com.sunsetbeach.entity.BookingEntity;
import com.sunsetbeach.entity.GuestAccountEntity;
import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.repository.BookingRepository;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.repository.GuestRepository;
import com.sunsetbeach.security.StaffPrincipal;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Makes {@code Guest} the one person record that {@code GuestAccount} and {@code Booking} both
 * point at, by finding or creating the Guest card for an email. The rule, everywhere:
 * <ul>
 *   <li>exactly one Guest has this email (trimmed, case-insensitive) - link to it, and never
 *       overwrite any of its fields with what was just typed; the CRM card's data wins;
 *   <li>more than one - link nothing. {@code Guest.email} has no unique constraint and duplicates
 *       exist; picking one would be a guess, and a wrong guess shows one person's stays to
 *       another. Staff can still link by hand ({@code PUT /bookings/{id}/guest});
 *   <li>none - create a card (when the caller allows it) and link to that.
 * </ul>
 * Merging duplicate cards is out of scope - see {@code Guest}'s own openapi.yaml description.
 */
@Service
public class GuestLinkService {

    private final GuestRepository guestRepository;
    private final GuestAccountRepository guestAccountRepository;
    private final BookingRepository bookingRepository;
    private final AuditLogService auditLogService;

    public GuestLinkService(
            GuestRepository guestRepository,
            GuestAccountRepository guestAccountRepository,
            BookingRepository bookingRepository,
            AuditLogService auditLogService) {
        this.guestRepository = guestRepository;
        this.guestAccountRepository = guestAccountRepository;
        this.bookingRepository = bookingRepository;
        this.auditLogService = auditLogService;
    }

    /**
     * Sets a just-created booking's {@code guestId} from its own {@code guestEmail}, unless it
     * already has one. Runs in its own transaction after the booking itself has committed - see
     * {@code BookingService#linkGuestQuietly} for why a failure here must not fail the booking.
     *
     * @return the linked Guest, or null if none could be (no email, or an ambiguous one)
     */
    @Transactional
    public GuestEntity linkNewBooking(String bookingId) {
        BookingEntity booking = bookingRepository.findById(bookingId).orElseThrow(() -> new NotFoundException("Booking not found"));
        if (booking.getGuestId() != null) {
            return guestRepository.findById(booking.getGuestId()).orElse(null);
        }
        GuestEntity guest = findOrCreate(booking.getGuestEmail(), booking.getGuestName(), booking.getGuestPhone(), true).orElse(null);
        if (guest != null) {
            booking.setGuestId(guest.getId());
            bookingRepository.saveAndFlush(booking);
        }
        return guest;
    }

    /**
     * Links an account to its Guest card if it isn't linked yet. Joins the caller's transaction,
     * so the link is atomic with the registration/verification that triggered it.
     *
     * @param allowCreate false at registration (an unverified signup - anyone can type anyone's
     *     email - may link to an existing card, but doesn't get to add one to the CRM), true at
     *     verification, once the email is proven
     */
    @Transactional
    public void linkAccount(GuestAccountEntity account, boolean allowCreate) {
        if (account.getGuestId() != null) {
            return;
        }
        String name = account.getName() != null && !account.getName().isBlank() ? account.getName() : account.getEmail();
        findOrCreate(account.getEmail(), name, null, allowCreate)
                // One account per Guest (V104's unique index). Only reachable if staff edited a
                // linked card's email to another account's address - leave this one unlinked.
                .filter(guest -> !guestAccountRepository.existsByGuestId(guest.getId()))
                .ifPresent(guest -> account.setGuestId(guest.getId()));
    }

    /**
     * Two concurrent callers with the same new email (a booking and a verification at once, or two
     * bookings) would otherwise both see "none" and both create a card - after which every later
     * lookup for that email is ambiguous and links nothing, forever. A transaction-scoped advisory
     * lock on the email serializes just that pair; there's no unique constraint on
     * {@code Guest.email} to lean on instead, because production already has duplicates.
     */
    private Optional<GuestEntity> findOrCreate(String rawEmail, String name, String phone, boolean allowCreate) {
        String email = normalize(rawEmail);
        if (email == null) {
            return Optional.empty();
        }
        guestRepository.lockEmail(email);
        List<GuestEntity> matches = guestRepository.findByNormalizedEmail(email);
        if (matches.size() == 1) {
            return Optional.of(matches.get(0));
        }
        if (!matches.isEmpty() || !allowCreate) {
            return Optional.empty();
        }

        GuestEntity guest = new GuestEntity();
        guest.setName(name != null && !name.isBlank() ? name.trim() : rawEmail.trim());
        guest.setEmail(rawEmail.trim());
        guest.setPhone(phone != null && !phone.isBlank() ? phone.trim() : null);
        GuestEntity saved = guestRepository.saveAndFlush(guest);
        recordCreated(saved);
        return Optional.of(saved);
    }

    // A public booking or a guest's own verification has no staff member behind it - see
    // AuditLogService#recordSystemAction.
    private void recordCreated(GuestEntity guest) {
        String summary = "Guest " + guest.getName() + " created automatically";
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof StaffPrincipal) {
            auditLogService.record(AuditAction.GUEST_CREATED, AuditEntityType.GUEST, guest.getId(), summary);
        } else {
            auditLogService.recordSystemAction(AuditAction.GUEST_CREATED, AuditEntityType.GUEST, guest.getId(), summary);
        }
    }

    /** Same normalization as {@code GuestAccountService} (trim + lowercase), plus blank to null. */
    static String normalize(String email) {
        if (email == null || email.isBlank()) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
