package com.sunsetbeach.service;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.ForbiddenException;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.ShiftRequestDeciders;
import com.sunsetbeach.repository.StaffChannelLinkRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Who decides employees' shift requests <em>today</em>. The active holder of the {@code HR} job function
 * (UserService keeps it at most one); while nobody holds it, or the holder is on leave (their own dates,
 * hotel calendar), the administrators decide - if the setting {@code fallbackToAdmin} is on - and if it is
 * off the requests simply wait. Asked afresh at every notification and every press of a button, so a
 * function taken away, an account deactivated or a leave that began is in force at once.
 */
@Service
public class ShiftRequestDeciderService {

    private static final String TELEGRAM = "TELEGRAM";

    private final UserRepository userRepository;
    private final StaffChannelLinkRepository linkRepository;
    private final StaffBotSettingsService settingsService;
    private final AuditLogService auditLogService;
    private final Clock clock;

    public ShiftRequestDeciderService(
            UserRepository userRepository,
            StaffChannelLinkRepository linkRepository,
            StaffBotSettingsService settingsService,
            AuditLogService auditLogService,
            Clock clock) {
        this.userRepository = userRepository;
        this.linkRepository = linkRepository;
        this.settingsService = settingsService;
        this.auditLogService = auditLogService;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Optional<UserEntity> activeHr() {
        return userRepository.findActiveHrHolders().stream().findFirst();
    }

    public static boolean onVacation(UserEntity hr, LocalDate today) {
        return hr.getHrVacationFrom() != null
                && !today.isBefore(hr.getHrVacationFrom())
                && !today.isAfter(hr.getHrVacationTo());
    }

    /** The people who may decide on {@code today}: the HR holder, or the administrators when there is none / they are away. */
    @Transactional(readOnly = true)
    public List<UserEntity> effectiveDeciders(LocalDate today) {
        Optional<UserEntity> hr = activeHr();
        if (hr.isPresent() && !onVacation(hr.get(), today)) {
            return List.of(hr.get());
        }
        if (!settingsService.getEntity().isFallbackToAdmin()) {
            return List.of();
        }
        return userRepository.findByRoleIn(List.of(Role.ADMIN)).stream().filter(UserEntity::isActive).toList();
    }

    /** Whether {@code user} holds the HR job function (a second axis next to the role, see {@code JobFunction}). */
    public static boolean holdsHr(UserEntity user) {
        return user != null && user.getJobFunctions() != null && java.util.Arrays.asList(user.getJobFunctions()).contains("HR");
    }

    /**
     * Who may decide a request filed by {@code employee}. The same people as {@link #effectiveDeciders} - except for
     * the HR director's <em>own</em> request: the director never decides their own, so it goes to the administrators
     * (as if nobody held HR) whatever {@code fallbackToAdmin} says, because with it off nobody could ever decide it.
     * An administrator who is themselves the filer is left out for the same reason.
     */
    @Transactional(readOnly = true)
    public List<UserEntity> decidersFor(UserEntity employee, LocalDate today) {
        if (holdsHr(employee)) {
            return userRepository.findByRoleIn(List.of(Role.ADMIN)).stream()
                    .filter(UserEntity::isActive)
                    .filter(a -> !a.getId().equals(employee.getId()))
                    .toList();
        }
        return effectiveDeciders(today);
    }

    /** Whether the person may decide this particular request: one of today's deciders, and not its filer. */
    @Transactional(readOnly = true)
    public boolean mayDecide(UserEntity user, String employeeUserId, LocalDate today) {
        if (user == null || !user.isActive() || user.getId().equals(employeeUserId)) {
            return false;
        }
        UserEntity employee = userRepository.findById(employeeUserId).orElse(null);
        return employee != null && decidersFor(employee, today).stream().anyMatch(d -> d.getId().equals(user.getId()));
    }

    /**
     * Whether this person decides the HR director's own requests without being one of today's deciders in general: an
     * administrator while an HR director exists (their requests come to the administrators, see {@link #decidersFor}).
     */
    @Transactional(readOnly = true)
    public boolean decidesTheHrDirectorsRequests(UserEntity user) {
        return user != null && user.isActive() && user.getRole() == Role.ADMIN
                && activeHr().filter(hr -> !hr.getId().equals(user.getId())).isPresent();
    }

    /** Whether this person is one of today's deciders at all (the screens that list requests); see {@link #mayDecide(UserEntity, String, LocalDate)} for a given request. */
    @Transactional(readOnly = true)
    public boolean mayDecide(UserEntity user, LocalDate today) {
        return user != null && user.isActive() && effectiveDeciders(today).stream().anyMatch(d -> d.getId().equals(user.getId()));
    }

    /**
     * Who is told about a new request: the HR holder (always, so the message is in their log even without a
     * linked chat), or - on the fallback - the administrators who have a Telegram chat linked.
     */
    @Transactional(readOnly = true)
    public List<String> notifyRecipientIds(UserEntity employee, LocalDate today) {
        List<UserEntity> deciders = decidersFor(employee, today);
        boolean fallback = deciders.stream().noneMatch(ShiftRequestDeciderService::holdsHr);
        return deciders.stream()
                .filter(d -> !fallback || linkRepository.findByUserIdAndChannel(d.getId(), TELEGRAM).isPresent())
                .map(UserEntity::getId)
                .toList();
    }

    @Transactional(readOnly = true)
    public ShiftRequestDeciders view(UserEntity caller) {
        LocalDate today = LocalDate.now(clock);
        Optional<UserEntity> hr = activeHr();
        ShiftRequestDeciders view = new ShiftRequestDeciders(
                hr.isPresent() && onVacation(hr.get(), today),
                settingsService.getEntity().isFallbackToAdmin(),
                effectiveDeciders(today).stream().map(UserEntity::getName).toList(),
                mayDecide(caller, today));
        hr.ifPresent(h -> {
            view.hrUserId(h.getId()).hrName(h.getName());
            if (h.getHrVacationFrom() != null) {
                view.vacationFrom(h.getHrVacationFrom().toString()).vacationTo(h.getHrVacationTo().toString());
            }
        });
        return view;
    }

    /**
     * Sets (or, with both dates null, clears) the HR holder's leave. The holder sets their own; an
     * administrator sets it for them. {@code via} (nullable) names the channel for the audit entry.
     */
    @Transactional
    public ShiftRequestDeciders setVacation(UserEntity caller, LocalDate from, LocalDate to, String via) {
        UserEntity hr = activeHr().orElseThrow(() -> new ConflictException("Nobody holds the HR function"));
        boolean isHolder = hr.getId().equals(caller.getId());
        if (!isHolder && caller.getRole() != Role.ADMIN) {
            throw new ForbiddenException("Only the HR director or an administrator can set the leave");
        }
        if ((from == null) != (to == null)) {
            throw new BadRequestException("Give both dates, or neither to clear the leave");
        }
        if (from != null && to.isBefore(from)) {
            throw new BadRequestException("The last day can't be before the first");
        }
        if (!Objects.equals(from, hr.getHrVacationFrom()) || !Objects.equals(to, hr.getHrVacationTo())) {
            String before = hr.getHrVacationFrom() == null ? "none" : hr.getHrVacationFrom() + ".." + hr.getHrVacationTo();
            String after = from == null ? "none" : from + ".." + to;
            hr.setHrVacationFrom(from);
            hr.setHrVacationTo(to);
            userRepository.saveAndFlush(hr);
            auditLogService.recordAs(
                    new AuditLogService.Actor(caller.getId(), caller.getEmail(), caller.getRole(), via),
                    AuditAction.HR_VACATION_CHANGED, AuditEntityType.USER, hr.getId(),
                    "Leave of the HR director " + hr.getName() + ": " + before + " -> " + after);
        }
        return view(caller);
    }
}
