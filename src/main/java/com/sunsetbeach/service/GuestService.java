package com.sunsetbeach.service;

import com.sunsetbeach.entity.GuestEntity;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.ValidationException;
import com.sunsetbeach.mapper.GuestMapper;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Guest;
import com.sunsetbeach.model.GuestCreateInput;
import com.sunsetbeach.model.GuestDetail;
import com.sunsetbeach.model.GuestUpdateInput;
import com.sunsetbeach.repository.GuestAccountRepository;
import com.sunsetbeach.repository.GuestEmailLogRepository;
import com.sunsetbeach.repository.GuestRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuestService {

    private final GuestRepository guestRepository;
    private final GuestAccountRepository guestAccountRepository;
    private final GuestEmailLogRepository guestEmailLogRepository;
    private final GuestMapper guestMapper;
    private final BookingService bookingService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public GuestService(
            GuestRepository guestRepository,
            GuestAccountRepository guestAccountRepository,
            GuestEmailLogRepository guestEmailLogRepository,
            GuestMapper guestMapper,
            BookingService bookingService,
            AuditLogService auditLogService,
            Clock clock) {
        this.guestRepository = guestRepository;
        this.guestAccountRepository = guestAccountRepository;
        this.guestEmailLogRepository = guestEmailLogRepository;
        this.guestMapper = guestMapper;
        this.bookingService = bookingService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    /** {@code q} blank or omitted returns every guest, newest first - see the operation's own description in openapi.yaml for why. */
    @Transactional(readOnly = true)
    public List<Guest> search(String q) {
        String pattern = q != null && !q.isBlank() ? "%" + q.trim().toLowerCase() + "%" : null;
        return guestRepository.search(pattern).stream().map(guestMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public GuestDetail getDetail(String id) {
        GuestEntity entity = findEntity(id);
        return guestMapper.toDetailDto(
                entity,
                bookingService.listByGuestId(id),
                guestAccountRepository.findByGuestId(id).orElse(null),
                guestEmailLogRepository.findByGuestIdOrderBySentAtDesc(id));
    }

    @Transactional
    public Guest create(GuestCreateInput input) {
        GuestEntity entity = new GuestEntity();
        guestMapper.applyCreate(entity, input);
        requireDateOfBirthNotInFuture(entity);
        GuestEntity saved = guestRepository.saveAndFlush(entity);
        auditLogService.record(AuditAction.GUEST_CREATED, AuditEntityType.GUEST, saved.getId(), "Guest " + saved.getName() + " created");
        return guestMapper.toDto(saved);
    }

    @Transactional
    public Guest update(String id, GuestUpdateInput input) {
        GuestEntity entity = findEntity(id);
        String oldName = entity.getName();
        boolean wasVip = entity.isVip();
        guestMapper.applyUpdate(entity, input);
        requireDateOfBirthNotInFuture(entity);
        GuestEntity saved = guestRepository.saveAndFlush(entity);
        boolean renamed = !oldName.equals(saved.getName());
        // A VIP change is called out on its own, same reasoning as UserService#updateName's
        // dedicated entry: it's the one field here someone scanning the log would look for.
        String vipChange = wasVip == saved.isVip() ? null : saved.isVip() ? "marked VIP" : "unmarked VIP";
        String summary;
        if (renamed) {
            summary = "Guest " + oldName + " renamed to " + saved.getName() + (vipChange != null ? "; " + vipChange : "");
        } else {
            summary = "Guest " + saved.getName() + " " + (vipChange != null ? vipChange : "updated");
        }
        auditLogService.record(AuditAction.GUEST_UPDATED, AuditEntityType.GUEST, saved.getId(), summary);
        return guestMapper.toDto(saved);
    }

    /** Today (hotel-local, via the shared {@link Clock}) is allowed; tomorrow is not. */
    private void requireDateOfBirthNotInFuture(GuestEntity entity) {
        if (entity.getDateOfBirth() != null && entity.getDateOfBirth().isAfter(LocalDate.now(clock))) {
            throw ValidationException.field("dateOfBirth", "Date of birth can't be in the future");
        }
    }

    /**
     * Blocked while any booking still references this guest - past or future, unlike
     * {@link RoomUnitService#delete}'s upcoming-only check - see the operation's own description
     * in openapi.yaml for why. No pre-check needed: {@code Booking.guestId}'s FK is RESTRICT,
     * so the database itself enforces this and the violation is translated here into a friendly
     * message, the same backstop pattern {@code RoomUnitService.delete} already uses.
     */
    @Transactional
    public void delete(String id) {
        GuestEntity entity = findEntity(id);
        try {
            guestRepository.delete(entity);
            guestRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This guest has bookings on record and can't be deleted.");
        }
        auditLogService.record(AuditAction.GUEST_DELETED, AuditEntityType.GUEST, id, "Guest " + entity.getName() + " deleted");
    }

    private GuestEntity findEntity(String id) {
        return guestRepository.findById(id).orElseThrow(() -> new NotFoundException("Guest not found"));
    }
}
