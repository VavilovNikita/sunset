package com.sunsetbeach.service;

import com.sunsetbeach.entity.RosterEntryEntity;
import com.sunsetbeach.entity.ShiftChangeRequestEntity;
import com.sunsetbeach.entity.ShiftCodeEntity;
import com.sunsetbeach.entity.StaffAreaCoverageRuleEntity;
import com.sunsetbeach.entity.StaffBotSettingsEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.mapper.TimestampFormat;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.NotificationLanguage;
import com.sunsetbeach.model.NotificationType;
import com.sunsetbeach.model.RosterMoveInput;
import com.sunsetbeach.model.ShiftChangeRequest;
import com.sunsetbeach.model.ShiftChangeRequestChannel;
import com.sunsetbeach.model.ShiftChangeRequestStatus;
import com.sunsetbeach.model.ShiftChangeRequestType;
import com.sunsetbeach.model.ShiftRequestCoverage;
import com.sunsetbeach.model.StaffArea;
import com.sunsetbeach.notification.BotText;
import com.sunsetbeach.notification.NotificationAction;
import com.sunsetbeach.notification.NotificationMessages;
import com.sunsetbeach.notification.NotificationMessages.Key;
import com.sunsetbeach.repository.RosterEntryRepository;
import com.sunsetbeach.repository.ShiftChangeRequestRepository;
import com.sunsetbeach.repository.ShiftCodeRepository;
import com.sunsetbeach.repository.StaffAreaCoverageRuleRepository;
import com.sunsetbeach.repository.UserRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * An employee's request to move or remove one of their shifts, and the decision on it.
 *
 * <p><b>An approval is applied by the roster's own code</b> ({@link RosterService#moveEntry} /
 * {@link RosterService#deleteEntry}), in the same transaction as the decision, with every check those
 * methods make - this class has no second way to change a roster. If the entry is no longer what it was when
 * the request was filed (moved, deleted, locked, another shift code, the employee deactivated) or the target
 * day is taken, nothing is applied and the request becomes {@code EXPIRED} ({@link ShiftRequestExpiry}).
 *
 * <p><b>Every status change is one conditional {@code UPDATE ... WHERE status = 'PENDING'}</b>, the way a
 * payment per order is a unique constraint and not a check: two deciders pressing at the same moment (the
 * bot and the admin screen, two administrators on the fallback) race on that row's lock, one wins and
 * applies the change, the other finds it settled and gets the current status back. A decision whose apply
 * step fails rolls the claim back with it and is then recorded as an expiry in a second transaction.
 *
 * <p>Coverage - how many of the employee's department still work on the day(s) concerned, against
 * {@code StaffAreaCoverageRule} - is information for the decider and never a reason to refuse.
 */
@Service
public class ShiftChangeRequestService {

    private static final Logger log = LoggerFactory.getLogger(ShiftChangeRequestService.class);

    public static final int MAX_REASON_LENGTH = 300;
    private static final String PENDING = "PENDING";
    private static final String REQUESTS_PATH = "/admin/shift-requests";

    /** Why a request was refused before it was filed - the bot words each in the person's language. */
    public enum Rejection {
        DISABLED, NOT_YOURS, NOT_A_SHIFT, LOCKED, PAST, TODAY, TOO_LATE, PENDING, TARGET, TARGET_BUSY, REASON_REQUIRED, REASON_TOO_LONG
    }

    public static class ShiftRequestException extends RuntimeException {
        private final Rejection reason;

        public ShiftRequestException(Rejection reason) {
            super("Shift request refused: " + reason);
            this.reason = reason;
        }

        public Rejection reason() {
            return reason;
        }
    }

    /** The request as it stands after a decision, and whether this call is what changed it. */
    public record DecisionResult(ShiftChangeRequestEntity request, boolean changedNow) {
    }

    private record Coverage(StaffArea area, int minimum, LocalDate sourceDate, int sourceAfter, LocalDate targetDate, Integer targetAfter) {
        boolean below() {
            return sourceAfter < minimum || (targetAfter != null && targetAfter < minimum);
        }
    }

    private final ShiftChangeRequestRepository requestRepository;
    private final RosterEntryRepository entryRepository;
    private final ShiftCodeRepository shiftCodeRepository;
    private final StaffAreaCoverageRuleRepository coverageRuleRepository;
    private final UserRepository userRepository;
    private final RosterService rosterService;
    private final RosterChangeTracker changeTracker;
    private final StaffBotSettingsService settingsService;
    private final ShiftRequestDeciderService deciderService;
    private final NotificationService notificationService;
    private final ShiftRequestMessageSync messageSync;
    private final AuditLogService auditLogService;
    private final Clock clock;
    private final TransactionTemplate tx;

    public ShiftChangeRequestService(
            ShiftChangeRequestRepository requestRepository,
            RosterEntryRepository entryRepository,
            ShiftCodeRepository shiftCodeRepository,
            StaffAreaCoverageRuleRepository coverageRuleRepository,
            UserRepository userRepository,
            RosterService rosterService,
            RosterChangeTracker changeTracker,
            StaffBotSettingsService settingsService,
            ShiftRequestDeciderService deciderService,
            NotificationService notificationService,
            ShiftRequestMessageSync messageSync,
            AuditLogService auditLogService,
            Clock clock,
            PlatformTransactionManager transactionManager) {
        this.requestRepository = requestRepository;
        this.entryRepository = entryRepository;
        this.shiftCodeRepository = shiftCodeRepository;
        this.coverageRuleRepository = coverageRuleRepository;
        this.userRepository = userRepository;
        this.rosterService = rosterService;
        this.changeTracker = changeTracker;
        this.settingsService = settingsService;
        this.deciderService = deciderService;
        this.notificationService = notificationService;
        this.messageSync = messageSync;
        this.auditLogService = auditLogService;
        this.clock = clock;
        this.tx = new TransactionTemplate(transactionManager);
    }

    // ---- may this be requested? ---------------------------------------------------------------------

    /** Why {@code entry} can't be the subject of a request of {@code type} right now, or empty when it can. */
    public Optional<Rejection> check(UserEntity employee, RosterEntryEntity entry, ShiftChangeRequestType type) {
        StaffBotSettingsEntity s = settingsService.getEntity();
        if (!s.isRequestsEnabled()
                || (type == ShiftChangeRequestType.MOVE && !s.isMoveRequestsEnabled())
                || (type == ShiftChangeRequestType.DELETE && !s.isDeleteRequestsEnabled())) {
            return Optional.of(Rejection.DISABLED);
        }
        if (!employee.isActive() || !entry.getEmployeeUserId().equals(employee.getId())) {
            return Optional.of(Rejection.NOT_YOURS);
        }
        ShiftCodeEntity code = shiftCodeRepository.findById(entry.getShiftCodeId()).orElse(null);
        if (code == null || !code.isCountsAsWorked()) {
            return Optional.of(Rejection.NOT_A_SHIFT);
        }
        if (entry.isLocked()) {
            return Optional.of(Rejection.LOCKED);
        }
        Optional<Rejection> timing = timingProblem(entry.getDate(), code, s);
        if (timing.isPresent()) {
            return timing;
        }
        if (!requestRepository.findByRosterEntryIdInAndStatus(List.of(entry.getId()), PENDING).isEmpty()) {
            return Optional.of(Rejection.PENDING);
        }
        return Optional.empty();
    }

    /** Past, today (unless allowed) and "too late" by the configured notice, all by the hotel clock. */
    private Optional<Rejection> timingProblem(LocalDate date, ShiftCodeEntity code, StaffBotSettingsEntity s) {
        LocalDate today = LocalDate.now(clock);
        if (date.isBefore(today)) {
            return Optional.of(Rejection.PAST);
        }
        if (date.equals(today) && !s.isAllowTodayRequests()) {
            return Optional.of(Rejection.TODAY);
        }
        // "No later than N hours before the shift starts": now + N hours must not pass the start.
        if (LocalDateTime.now(clock).plusHours(s.getRequestMinHoursBefore()).isAfter(ShiftLabels.start(date, code))) {
            return Optional.of(Rejection.TOO_LATE);
        }
        return Optional.empty();
    }

    /** Days a MOVE can go to: inside the window, free for the employee, and acceptable by the same timing rule. */
    public List<LocalDate> eligibleTargets(UserEntity employee, RosterEntryEntity entry) {
        StaffBotSettingsEntity s = settingsService.getEntity();
        ShiftCodeEntity code = shiftCodeRepository.findById(entry.getShiftCodeId()).orElse(null);
        LocalDate today = LocalDate.now(clock);
        LocalDate last = today.plusDays(s.getMoveWindowDays());
        Set<LocalDate> busy = entryRepository.findByEmployeeUserIdAndDateBetween(employee.getId(), today, last).stream()
                .map(RosterEntryEntity::getDate).collect(Collectors.toSet());
        List<LocalDate> dates = new ArrayList<>();
        for (LocalDate d = today; !d.isAfter(last); d = d.plusDays(1)) {
            if (!d.equals(entry.getDate()) && !busy.contains(d) && timingProblem(d, code, s).isEmpty()) {
                dates.add(d);
            }
        }
        return dates;
    }

    public Optional<Rejection> checkTarget(UserEntity employee, RosterEntryEntity entry, LocalDate target) {
        StaffBotSettingsEntity s = settingsService.getEntity();
        LocalDate today = LocalDate.now(clock);
        if (target == null || target.isBefore(today) || target.isAfter(today.plusDays(s.getMoveWindowDays())) || target.equals(entry.getDate())) {
            return Optional.of(Rejection.TARGET);
        }
        if (entryRepository.findByEmployeeUserIdAndDate(employee.getId(), target).isPresent()) {
            return Optional.of(Rejection.TARGET_BUSY);
        }
        return timingProblem(target, shiftCodeRepository.findById(entry.getShiftCodeId()).orElse(null), s);
    }

    // ---- filing --------------------------------------------------------------------------------------

    /** Files a request. Throws {@link ShiftRequestException} for every rule it breaks. */
    public ShiftChangeRequestEntity submit(
            UserEntity employee, String entryId, ShiftChangeRequestType type, LocalDate targetDate, String reasonText, ShiftChangeRequestChannel channel) {
        StaffBotSettingsEntity s = settingsService.getEntity();
        RosterEntryEntity entry = entryRepository.findById(entryId).orElseThrow(() -> new ShiftRequestException(Rejection.NOT_YOURS));
        check(employee, entry, type).ifPresent(r -> {
            throw new ShiftRequestException(r);
        });
        if (type == ShiftChangeRequestType.MOVE) {
            checkTarget(employee, entry, targetDate).ifPresent(r -> {
                throw new ShiftRequestException(r);
            });
        }
        String reason = reasonText == null || reasonText.isBlank() ? null : reasonText.strip();
        if (reason != null && reason.length() > MAX_REASON_LENGTH) {
            throw new ShiftRequestException(Rejection.REASON_TOO_LONG);
        }
        if (reason == null && s.isReasonRequired()) {
            throw new ShiftRequestException(Rejection.REASON_REQUIRED);
        }
        ShiftCodeEntity code = shiftCodeRepository.findById(entry.getShiftCodeId()).orElseThrow();

        ShiftChangeRequestEntity request = new ShiftChangeRequestEntity();
        request.setEmployeeUserId(employee.getId());
        request.setRosterEntryId(entry.getId());
        request.setType(type.getValue());
        request.setChannel(channel.getValue());
        request.setReason(reason);
        request.setShiftDate(entry.getDate());
        request.setShiftCodeId(code.getId());
        request.setShiftCode(code.getCode());
        request.setShiftTimes(ShiftLabels.times(code));
        request.setTargetDate(type == ShiftChangeRequestType.MOVE ? targetDate : null);
        ShiftChangeRequestEntity saved;
        try {
            // The unique index (one PENDING request per entry) is what makes two taps at once file one.
            saved = tx.execute(status -> requestRepository.saveAndFlush(request));
        } catch (DataIntegrityViolationException e) {
            throw new ShiftRequestException(Rejection.PENDING);
        }

        auditLogService.recordAs(actor(employee, channel), AuditAction.SHIFT_CHANGE_REQUESTED, AuditEntityType.SHIFT_CHANGE_REQUEST, saved.getId(),
                employee.getName() + " asked to " + describe(saved, NotificationLanguage.EN) + (reason == null ? "" : ". Reason: " + reason));
        try {
            notifyDeciders(saved, employee, NotificationType.SHIFT_REQUEST_NEW, Key.SHIFT_REQUEST_NEW, "SHIFTREQ_NEW:" + saved.getId(), 0);
        } catch (RuntimeException e) {
            log.error("Telling the deciders about shift request {} failed", saved.getId(), e);
        }
        return saved;
    }

    // ---- deciding --------------------------------------------------------------------------------------

    /**
     * Approves or rejects. Idempotent: a request that is no longer {@code PENDING} is returned as it is.
     *
     * @throws ForbiddenException when {@code decider} is not one of today's deciders
     */
    public DecisionResult decide(String requestId, UserEntity decider, boolean approve, String comment, ShiftChangeRequestChannel channel) {
        ShiftChangeRequestEntity current = requestRepository.findById(requestId).orElseThrow(() -> new NotFoundException("Shift request not found"));
        if (!deciderService.mayDecide(decider, current.getEmployeeUserId(), LocalDate.now(clock))) {
            throw new ForbiddenException("You are not one of the people who decide this shift request right now");
        }
        if (!PENDING.equals(current.getStatus())) {
            return new DecisionResult(current, false);
        }
        String note = comment == null || comment.isBlank() ? null : comment.strip();
        if (note != null && note.length() > MAX_REASON_LENGTH) {
            throw new BadRequestException("The comment is too long (up to " + MAX_REASON_LENGTH + " characters)");
        }

        boolean changed;
        if (!approve) {
            changed = tx.execute(status -> requestRepository.decide(requestId, "REJECTED", decider.getId(), note, TimestampFormat.nowUtc(clock)) == 1);
        } else {
            changed = approveAndApply(current, decider, note, channel);
        }
        ShiftChangeRequestEntity after = requestRepository.findById(requestId).orElseThrow();
        if (changed) {
            afterSettled(after, decider, channel);
        }
        return new DecisionResult(after, changed);
    }

    /** Claim + validate + apply in one transaction; a validation or apply failure rolls the claim back and expires instead. */
    private boolean approveAndApply(ShiftChangeRequestEntity request, UserEntity decider, String note, ShiftChangeRequestChannel channel) {
        class Expire extends RuntimeException {
            final ShiftRequestExpiry reason;

            Expire(ShiftRequestExpiry reason) {
                super(reason.name(), null, false, false);
                this.reason = reason;
            }
        }
        try {
            return Boolean.TRUE.equals(tx.execute(status -> {
                // The row lock is the race: the second decider waits here and then finds nothing PENDING.
                if (requestRepository.decide(request.getId(), "APPROVED", decider.getId(), note, TimestampFormat.nowUtc(clock)) == 0) {
                    return false;
                }
                ShiftRequestExpiry problem = stillApplicable(request);
                if (problem != null) {
                    throw new Expire(problem);
                }
                try {
                    AuditLogService.runAs(actor(decider, channel), () -> changeTracker.suppressed(() -> {
                        if (ShiftChangeRequestType.DELETE.getValue().equals(request.getType())) {
                            rosterService.deleteEntry(request.getRosterEntryId());
                        } else {
                            rosterService.moveEntry(request.getRosterEntryId(), new RosterMoveInput(request.getTargetDate().toString()), decider.getId());
                        }
                        return null;
                    }));
                } catch (BadRequestException | NotFoundException e) {
                    log.info("Shift request {} could not be applied: {}", request.getId(), e.getMessage());
                    throw new Expire(ShiftRequestExpiry.APPLY_FAILED);
                }
                return true;
            }));
        } catch (Expire e) {
            Boolean expired = tx.execute(status -> requestRepository.expire(request.getId(), e.reason.name(), decider.getId(), TimestampFormat.nowUtc(clock)) == 1);
            return Boolean.TRUE.equals(expired);
        }
    }

    /** Null when the entry still looks like it did at filing, otherwise why the request can no longer be applied. Locks the entry. */
    private ShiftRequestExpiry stillApplicable(ShiftChangeRequestEntity r) {
        RosterEntryEntity entry = r.getRosterEntryId() == null ? null : entryRepository.findByIdForUpdate(r.getRosterEntryId()).orElse(null);
        if (entry == null) {
            return ShiftRequestExpiry.ENTRY_GONE;
        }
        if (!entry.getEmployeeUserId().equals(r.getEmployeeUserId()) || !entry.getDate().equals(r.getShiftDate()) || !entry.getShiftCodeId().equals(r.getShiftCodeId())) {
            return ShiftRequestExpiry.ENTRY_CHANGED;
        }
        if (entry.isLocked()) {
            return ShiftRequestExpiry.LOCKED;
        }
        UserEntity employee = userRepository.findById(r.getEmployeeUserId()).orElse(null);
        if (employee == null || !employee.isActive()) {
            return ShiftRequestExpiry.EMPLOYEE_INACTIVE;
        }
        ShiftCodeEntity code = shiftCodeRepository.findById(r.getShiftCodeId()).orElse(null);
        if (!LocalDateTime.now(clock).isBefore(ShiftLabels.start(r.getShiftDate(), code))) {
            return ShiftRequestExpiry.SHIFT_STARTED;
        }
        if (r.getTargetDate() != null
                && (r.getTargetDate().isBefore(LocalDate.now(clock)) || entryRepository.findByEmployeeUserIdAndDate(r.getEmployeeUserId(), r.getTargetDate()).isPresent())) {
            return ShiftRequestExpiry.TARGET_TAKEN;
        }
        return null;
    }

    // ---- withdrawing, expiring -------------------------------------------------------------------------

    /** The employee withdraws their own request, or one of today's deciders does. Anything not PENDING is returned unchanged. */
    public DecisionResult cancel(String requestId, UserEntity by, ShiftChangeRequestChannel channel) {
        ShiftChangeRequestEntity current = requestRepository.findById(requestId).orElseThrow(() -> new NotFoundException("Shift request not found"));
        boolean own = current.getEmployeeUserId().equals(by.getId());
        if (!by.isActive() || (!own && !deciderService.mayDecide(by, current.getEmployeeUserId(), LocalDate.now(clock)))) {
            throw new ForbiddenException("You can't withdraw this request");
        }
        boolean changed = Boolean.TRUE.equals(tx.execute(status -> requestRepository.cancel(requestId, by.getId(), TimestampFormat.nowUtc(clock)) == 1));
        ShiftChangeRequestEntity after = requestRepository.findById(requestId).orElseThrow();
        if (changed) {
            afterSettled(after, by, channel);
        }
        return new DecisionResult(after, changed);
    }

    /** Expires every PENDING request whose shift has begun. Returns how many. Called by the sweep. */
    public int expireStarted() {
        int expired = 0;
        LocalDateTime now = LocalDateTime.now(clock);
        for (ShiftChangeRequestEntity r : requestRepository.findByStatus(PENDING)) {
            ShiftCodeEntity code = shiftCodeRepository.findById(r.getShiftCodeId()).orElse(null);
            if (now.isBefore(ShiftLabels.start(r.getShiftDate(), code))) {
                continue;
            }
            try {
                boolean changed = Boolean.TRUE.equals(tx.execute(status ->
                        requestRepository.expire(r.getId(), ShiftRequestExpiry.SHIFT_STARTED.name(), null, TimestampFormat.nowUtc(clock)) == 1));
                if (changed) {
                    expired++;
                    afterSettled(requestRepository.findById(r.getId()).orElseThrow(), null, null);
                }
            } catch (RuntimeException e) {
                log.error("Expiring shift request {} failed", r.getId(), e);
            }
        }
        return expired;
    }

    /** A reminder to the deciders for each request that has waited {@code pendingReminderHours}; once a hotel day per request. */
    public int remindPending() {
        StaffBotSettingsEntity s = settingsService.getEntity();
        if (!s.isRequestsEnabled() || !s.isPendingReminderEnabled()) {
            return 0;
        }
        LocalDate today = LocalDate.now(clock);
        LocalDateTime dueBefore = TimestampFormat.nowUtc(clock).minusHours(s.getPendingReminderHours());
        int sent = 0;
        for (ShiftChangeRequestEntity r : requestRepository.findByStatus(PENDING)) {
            if (r.getCreatedAt().isAfter(dueBefore)) {
                continue;
            }
            try {
                UserEntity employee = userRepository.findById(r.getEmployeeUserId()).orElse(null);
                if (employee == null) {
                    continue;
                }
                long hours = java.time.Duration.between(r.getCreatedAt(), TimestampFormat.nowUtc(clock)).toHours();
                sent += notifyDeciders(r, employee, NotificationType.SHIFT_REQUEST_REMINDER, Key.SHIFT_REQUEST_REMINDER,
                        "SHIFTREQ_REMIND:" + r.getId() + ":" + today, hours);
            } catch (RuntimeException e) {
                log.error("Reminding about shift request {} failed", r.getId(), e);
            }
        }
        return sent;
    }

    // ---- after a status change --------------------------------------------------------------------------

    private void afterSettled(ShiftChangeRequestEntity r, UserEntity by, ShiftChangeRequestChannel channel) {
        try {
            UserEntity employee = userRepository.findById(r.getEmployeeUserId()).orElse(null);
            String who = employee == null ? r.getEmployeeUserId() : employee.getName();
            AuditAction action = switch (r.getStatus()) {
                case "APPROVED" -> AuditAction.SHIFT_CHANGE_APPROVED;
                case "REJECTED" -> AuditAction.SHIFT_CHANGE_REJECTED;
                case "CANCELLED" -> AuditAction.SHIFT_CHANGE_CANCELLED;
                default -> AuditAction.SHIFT_CHANGE_EXPIRED;
            };
            String summary = who + "'s request to " + describe(r, NotificationLanguage.EN) + ": " + r.getStatus().toLowerCase()
                    + (r.getDecisionComment() == null ? "" : ". Comment: " + r.getDecisionComment())
                    + (r.getExpiredReason() == null ? "" : ". " + expiryText(r.getExpiredReason(), NotificationLanguage.EN));
            if (by == null) {
                auditLogService.recordSystemAction(action, AuditEntityType.SHIFT_CHANGE_REQUEST, r.getId(), summary);
            } else {
                auditLogService.recordAs(actor(by, channel), action, AuditEntityType.SHIFT_CHANGE_REQUEST, r.getId(), summary);
            }
            if (employee != null && ("APPROVED".equals(r.getStatus()) || "REJECTED".equals(r.getStatus()))) {
                notifyEmployeeDecided(r, employee, by);
            } else if (employee != null && "EXPIRED".equals(r.getStatus())) {
                notifyEmployeeExpired(r, employee);
            }
        } catch (RuntimeException e) {
            log.error("Following up shift request {} failed", r.getId(), e);
        }
        messageSync.settled(r);
    }

    private void notifyEmployeeDecided(ShiftChangeRequestEntity r, UserEntity employee, UserEntity by) {
        boolean approved = "APPROVED".equals(r.getStatus());
        Map<String, String> params = baseParams(r, employee);
        params.put("by", by == null ? "" : by.getName());
        BotText.perLanguage(params, "commentLine", l -> r.getDecisionComment() == null ? ""
                : NotificationMessages.render(Key.FRAG_COMMENT, l, Map.of("comment", r.getDecisionComment())));
        notificationService.raise(
                NotificationType.SHIFT_REQUEST_DECIDED,
                approved ? Key.SHIFT_REQUEST_DECIDED_APPROVED : Key.SHIFT_REQUEST_DECIDED_REJECTED,
                List.of(employee.getId()), params, "SHIFTREQ_DECIDED:" + r.getId());
    }

    private void notifyEmployeeExpired(ShiftChangeRequestEntity r, UserEntity employee) {
        Map<String, String> params = baseParams(r, employee);
        ShiftRequestExpiry why = ShiftRequestExpiry.of(r.getExpiredReason());
        BotText.perLanguage(params, "why", l -> why == null ? "" : why.text(l));
        notificationService.raise(NotificationType.SHIFT_REQUEST_EXPIRED, Key.SHIFT_REQUEST_EXPIRED, List.of(employee.getId()), params, "SHIFTREQ_EXPIRED:" + r.getId());
    }

    /** @return how many deciders were newly told */
    private int notifyDeciders(ShiftChangeRequestEntity r, UserEntity employee, NotificationType type, Key key, String dedupKey, long hours) {
        List<String> recipients = deciderService.notifyRecipientIds(employee, LocalDate.now(clock));
        if (recipients.isEmpty()) {
            return 0;
        }
        Map<String, String> params = baseParams(r, employee);
        params.put("hours", String.valueOf(hours));
        BotText.perLanguage(params, "reasonLine", l -> r.getReason() == null ? "" : NotificationMessages.render(Key.FRAG_REASON, l, Map.of("reason", r.getReason())));
        Coverage coverage = coverage(r, employee);
        BotText.perLanguage(params, "coverageLines", l -> coverage == null ? "" : coverageText(coverage, l));
        params.put(NotificationService.PATH, REQUESTS_PATH);
        return notificationService.raise(type, key, recipients, params, dedupKey, List.of(
                NotificationAction.of(NotificationAction.Kinds.SHIFT_REQUEST_APPROVE, r.getId()),
                NotificationAction.of(NotificationAction.Kinds.SHIFT_REQUEST_REJECT, r.getId())));
    }

    private Map<String, String> baseParams(ShiftChangeRequestEntity r, UserEntity employee) {
        Map<String, String> params = new HashMap<>();
        params.put("name", employee.getName());
        BotText.perLanguage(params, "what", l -> describe(r, l));
        return params;
    }

    /** "move the shift on Mon 20 Oct (N1 09:00-18:00) to Wed 22 Oct" / "take Mon 20 Oct off (shift N1 ...)" in {@code language}. */
    public String describe(ShiftChangeRequestEntity r, NotificationLanguage language) {
        Map<String, String> params = new HashMap<>();
        params.put("date", BotText.date(r.getShiftDate(), language));
        params.put("shift", BotText.shift(r.getShiftCode(), r.getShiftTimes()));
        if (r.getTargetDate() != null) {
            params.put("target", BotText.date(r.getTargetDate(), language));
        }
        return NotificationMessages.render(r.getTargetDate() != null ? Key.FRAG_WHAT_MOVE : Key.FRAG_WHAT_DELETE, language, params);
    }

    private static String expiryText(String code, NotificationLanguage language) {
        ShiftRequestExpiry why = ShiftRequestExpiry.of(code);
        return why == null ? "" : why.text(language);
    }

    private AuditLogService.Actor actor(UserEntity user, ShiftChangeRequestChannel channel) {
        return new AuditLogService.Actor(user.getId(), user.getEmail() == null ? user.getName() : user.getEmail(), user.getRole(),
                channel == ShiftChangeRequestChannel.TELEGRAM ? "Telegram" : null);
    }

    // ---- coverage ---------------------------------------------------------------------------------------

    /** Null when the employee has no department, the department no minimum, or the request is no longer waiting. */
    private Coverage coverage(ShiftChangeRequestEntity r, UserEntity employee) {
        StaffArea area = employee.getStaffArea();
        if (area == null || !PENDING.equals(r.getStatus())) {
            return null;
        }
        StaffAreaCoverageRuleEntity rule = coverageRuleRepository.findById(area).orElse(null);
        if (rule == null) {
            return null;
        }
        ShiftCodeEntity code = shiftCodeRepository.findById(r.getShiftCodeId()).orElse(null);
        boolean counts = RosterService.countsTowardCoverage(code, employee);
        boolean entryStillThere = r.getRosterEntryId() != null && entryRepository.findById(r.getRosterEntryId())
                .filter(e -> e.getDate().equals(r.getShiftDate()) && e.getEmployeeUserId().equals(employee.getId())).isPresent();
        int sourceAfter = working(area, r.getShiftDate()) - (counts && entryStillThere ? 1 : 0);
        Integer targetAfter = r.getTargetDate() == null ? null : working(area, r.getTargetDate()) + (counts ? 1 : 0);
        return new Coverage(area, rule.getMinimumWorking(), r.getShiftDate(), sourceAfter, r.getTargetDate(), targetAfter);
    }

    /** How many people of {@code area} work on {@code date} - the roster grid's own count. */
    private int working(StaffArea area, LocalDate date) {
        List<RosterEntryEntity> day = entryRepository.findByDateBetween(date, date);
        if (day.isEmpty()) {
            return 0;
        }
        Map<String, ShiftCodeEntity> codes = shiftCodeRepository.findAllById(day.stream().map(RosterEntryEntity::getShiftCodeId).distinct().toList()).stream()
                .collect(Collectors.toMap(ShiftCodeEntity::getId, Function.identity()));
        Map<String, UserEntity> users = userRepository.findAllById(day.stream().map(RosterEntryEntity::getEmployeeUserId).distinct().toList()).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));
        return (int) day.stream()
                .filter(e -> {
                    UserEntity u = users.get(e.getEmployeeUserId());
                    return RosterService.countsTowardCoverage(codes.get(e.getShiftCodeId()), u) && u.getStaffArea() == area;
                })
                .count();
    }

    private String coverageText(Coverage c, NotificationLanguage l) {
        StringBuilder out = new StringBuilder();
        out.append(coverageLine(c.area(), c.minimum(), c.sourceDate(), c.sourceAfter(), l));
        if (c.targetDate() != null) {
            out.append(coverageLine(c.area(), c.minimum(), c.targetDate(), c.targetAfter(), l));
        }
        return out.toString();
    }

    private static String coverageLine(StaffArea area, int minimum, LocalDate date, int working, NotificationLanguage l) {
        return NotificationMessages.render(working < minimum ? Key.FRAG_COVERAGE_BELOW : Key.FRAG_COVERAGE_OK, l, Map.of(
                "area", area.getValue(), "working", String.valueOf(working), "date", BotText.date(date, l), "minimum", String.valueOf(minimum)));
    }

    /** The request as a decider reads it - who, what, why and the coverage - in one language (the bot's list of waiting requests). */
    public String summaryText(ShiftChangeRequestEntity r, NotificationLanguage language) {
        UserEntity employee = userRepository.findById(r.getEmployeeUserId()).orElse(null);
        Map<String, String> params = new HashMap<>();
        params.put("name", employee == null ? "" : employee.getName());
        params.put("what", describe(r, language));
        params.put("reasonLine", r.getReason() == null ? "" : NotificationMessages.render(Key.FRAG_REASON, language, Map.of("reason", r.getReason())));
        Coverage coverage = employee == null ? null : coverage(r, employee);
        params.put("coverageLines", coverage == null ? "" : coverageText(coverage, language));
        return NotificationMessages.render(Key.PENDING_ITEM, language, params);
    }

    // ---- reads (admin API) ------------------------------------------------------------------------------

    public List<ShiftChangeRequest> list(
            ShiftChangeRequestStatus status, LocalDate from, LocalDate to, String employeeUserId, UserEntity caller) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BadRequestException("from must be on or before to");
        }
        Specification<ShiftChangeRequestEntity> spec = (root, query, cb) -> {
            List<Predicate> p = new ArrayList<>();
            if (status != null) {
                p.add(cb.equal(root.get("status"), status.getValue()));
            }
            if (from != null) {
                p.add(cb.greaterThanOrEqualTo(root.get("shiftDate"), from));
            }
            if (to != null) {
                p.add(cb.lessThanOrEqualTo(root.get("shiftDate"), to));
            }
            if (employeeUserId != null && !employeeUserId.isBlank()) {
                p.add(cb.equal(root.get("employeeUserId"), employeeUserId));
            }
            return cb.and(p.toArray(new Predicate[0]));
        };
        List<ShiftChangeRequestEntity> rows = requestRepository.findAll(spec, PageRequest.of(0, 200, Sort.by(Sort.Direction.DESC, "createdAt"))).getContent();
        return rows.stream().map(r -> toDto(r, caller)).toList();
    }

    public ShiftChangeRequest toDto(ShiftChangeRequestEntity r, UserEntity caller) {
        UserEntity employee = userRepository.findById(r.getEmployeeUserId()).orElse(null);
        ShiftChangeRequest dto = new ShiftChangeRequest(
                r.getId(), r.getEmployeeUserId(), employee == null ? "" : employee.getName(),
                ShiftChangeRequestType.fromValue(r.getType()), ShiftChangeRequestStatus.fromValue(r.getStatus()),
                ShiftChangeRequestChannel.fromValue(r.getChannel()), r.getShiftDate().toString(), r.getShiftCode(),
                TimestampFormat.toUtc(r.getCreatedAt()),
                PENDING.equals(r.getStatus()) && caller != null && deciderService.mayDecide(caller, r.getEmployeeUserId(), LocalDate.now(clock)),
                ShiftRequestDeciderService.holdsHr(employee));
        if (r.getRosterEntryId() != null) {
            dto.rosterEntryId(r.getRosterEntryId());
        }
        if (r.getReason() != null) {
            dto.reason(r.getReason());
        }
        if (r.getShiftTimes() != null) {
            dto.shiftTimes(r.getShiftTimes());
        }
        if (r.getTargetDate() != null) {
            dto.targetDate(r.getTargetDate().toString());
        }
        if (r.getDecidedAt() != null) {
            dto.decidedAt(TimestampFormat.toUtc(r.getDecidedAt()));
        }
        if (r.getDecidedByUserId() != null) {
            dto.decidedByUserId(r.getDecidedByUserId());
            userRepository.findById(r.getDecidedByUserId()).ifPresent(u -> dto.decidedByName(u.getName()));
        }
        if (r.getDecisionComment() != null) {
            dto.decisionComment(r.getDecisionComment());
        }
        if (r.getExpiredReason() != null) {
            dto.expiredReason(expiryText(r.getExpiredReason(), NotificationLanguage.EN));
        }
        if (employee != null) {
            Coverage c = coverage(r, employee);
            if (c != null) {
                ShiftRequestCoverage cov = new ShiftRequestCoverage(c.area(), c.minimum(), c.sourceDate().toString(), c.sourceAfter(), c.below());
                if (c.targetDate() != null) {
                    cov.targetDate(c.targetDate().toString()).targetWorkingAfter(c.targetAfter());
                }
                dto.coverage(cov);
            }
        }
        return dto;
    }
}
