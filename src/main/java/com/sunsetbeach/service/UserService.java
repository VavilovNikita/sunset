package com.sunsetbeach.service;

import com.sunsetbeach.entity.SpaAppointmentEntity;
import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.UnauthorizedException;
import com.sunsetbeach.mapper.UserMapper;
import com.sunsetbeach.model.AuditAction;
import com.sunsetbeach.model.AuditEntityType;
import com.sunsetbeach.model.JobFunction;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.SpaAppointmentStatus;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserCreateInput;
import com.sunsetbeach.model.UserFunctionsUpdateInput;
import com.sunsetbeach.model.UserUpdateResult;
import com.sunsetbeach.model.UserRoleUpdateInput;
import com.sunsetbeach.repository.SpaAppointmentRepository;
import com.sunsetbeach.repository.UserRepository;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;
    private final SpaAppointmentRepository spaAppointmentRepository;

    public UserService(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            AuditLogService auditLogService,
            SpaAppointmentRepository spaAppointmentRepository) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.auditLogService = auditLogService;
        this.spaAppointmentRepository = spaAppointmentRepository;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return userRepository.findAll().stream().map(userMapper::toDto).toList();
    }

    /**
     * {@code email}/{@code password} travel together or not at all - a staff member who never
     * signs in (most of the roster: cooks, housekeepers, anyone who exists so the roster/
     * attendance/pay-rate records have someone to point at) gets neither, rather than a
     * fabricated mailbox and a password nobody knows. Can't be expressed as openapi.yaml
     * `required` (there's no "exactly one of A and B, or neither" in JSON Schema without an
     * awkward `oneOf`/`not` combination that would produce a worse error message than this one
     * check), so it's validated here instead. See {@link #grantCredentials} for turning a
     * no-login account into a login-capable one later, without recreating it.
     */
    @Transactional
    public User create(UserCreateInput input) {
        if ((input.getEmail() == null) != (input.getPassword() == null)) {
            throw new BadRequestException("email and password must both be present or both be absent");
        }

        UserEntity entity = new UserEntity();
        entity.setName(input.getName().trim());
        if (input.getEmail() != null) {
            entity.setEmail(input.getEmail().trim());
            entity.setPasswordHash(passwordEncoder.encode(input.getPassword()));
        }
        entity.setRole(input.getRole() != null ? input.getRole() : Role.MANAGER);
        entity.setOvertimeEligible(input.getOvertimeEligible() != null ? input.getOvertimeEligible() : true);
        entity.setEnrollmentNumber(input.getEnrollmentNumber());

        UserEntity saved;
        try {
            saved = userRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw conflictForUniqueViolation(e);
        }
        auditLogService.record(
                AuditAction.USER_CREATED, AuditEntityType.USER, saved.getId(),
                "User " + saved.getName() + " created with role " + saved.getRole().getValue()
                        + (saved.getEmail() != null ? " (" + saved.getEmail() + ")" : " (no login credentials)"));
        return userMapper.toDto(saved);
    }

    /**
     * {@code User} has two independent unique columns (email, enrollmentNumber) that can each
     * reject a {@code saveAndFlush} - distinguishes which one actually fired by the violated
     * constraint's own name, read from the driver's own root-cause message via Spring's {@code
     * getMostSpecificCause()} (the same "recognise by the structured fact, not by guessing"
     * instinct as {@code SqlStates}, just at the constraint-name granularity that SQLSTATE alone
     * can't give here - both violations share the same SQLSTATE, 23505).
     */
    private static ConflictException conflictForUniqueViolation(DataIntegrityViolationException e) {
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.contains("User_enrollmentNumber_key")
                ? new ConflictException("This enrollment number is already assigned to another employee")
                : new ConflictException("A user with that email already exists");
    }

    /**
     * {@code PATCH /users/{id}/credentials} - the no-login-to-login transition named in
     * {@link #create}'s own comment (a dishwasher who becomes a receptionist). Only valid while
     * the target has no email yet; changing an *existing* login email is deliberately not this
     * method's job (see the operation's own openapi.yaml description for why) - reaching for this
     * to fix a typo'd email would silently skip whatever a dedicated change-email operation ought
     * to do (e.g. notifying the affected person), which doesn't exist today because nothing has
     * needed it yet.
     */
    @Transactional
    public User grantCredentials(String id, String email, String password) {
        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        if (entity.getEmail() != null) {
            throw new ConflictException("This user already has login credentials");
        }

        entity.setEmail(email.trim());
        entity.setPasswordHash(passwordEncoder.encode(password));

        UserEntity saved;
        try {
            saved = userRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A user with that email already exists");
        }
        auditLogService.record(
                AuditAction.USER_CREDENTIALS_GRANTED, AuditEntityType.USER, saved.getId(),
                "Login credentials granted to " + saved.getName() + " (" + saved.getEmail() + ")");
        return userMapper.toDto(saved);
    }

    @Transactional
    public User updateRole(String id, String callerId, UserRoleUpdateInput input) {
        if (id.equals(callerId)) {
            throw new BadRequestException("You can't change your own role");
        }

        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        Role oldRole = entity.getRole();
        entity.setRole(input.getRole());
        // A token issued before this change still carries the old role in its own "role" claim
        // (JwtAuthFilter builds authorities from the token, not a fresh DB read) - bumping
        // tokenVersion is what forces that token to fail verification on its next request so a
        // re-login is required to pick up the new role.
        entity.setTokenVersion(entity.getTokenVersion() + 1);
        UserEntity saved = userRepository.save(entity);
        auditLogService.record(
                AuditAction.USER_ROLE_CHANGED,
                AuditEntityType.USER,
                saved.getId(),
                "Role for " + saved.getEmail() + " changed from " + oldRole.getValue() + " to " + saved.getRole().getValue());
        return userMapper.toDto(saved);
    }

    /**
     * {@code PATCH /users/{id}/functions} - job functions are a second, independent
     * authorization axis alongside {@link Role} (see {@link JobFunction}), so unlike
     * {@link #updateRole}/{@link #setActive}/{@link #resetPassword} this does NOT bump
     * {@code tokenVersion}: {@link com.sunsetbeach.security.JwtAuthFilter} already re-reads this
     * user's row on every request (to check {@code active}/{@code tokenVersion}) and grants
     * {@code FUNCTION_<name>} authorities straight from that fresh read, not from the JWT's own
     * claims the way {@code role} is - so a change here is already enforced on the very next
     * request without forcing a re-login. Full replace, not incremental: the input is the
     * complete desired set. No self-change restriction - unlike role/active, this can't lock the
     * caller out of anything.
     */
    @Transactional
    public UserUpdateResult updateFunctions(String id, UserFunctionsUpdateInput input) {
        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        String[] oldFunctions = entity.getJobFunctions();
        String[] newFunctions = new LinkedHashSet<>(input.getFunctions()).stream().map(JobFunction::getValue).toArray(String[]::new);
        entity.setJobFunctions(newFunctions);
        UserEntity saved = userRepository.save(entity);
        auditLogService.record(
                AuditAction.USER_FUNCTIONS_CHANGED,
                AuditEntityType.USER,
                saved.getId(),
                "Job functions for " + saved.getEmail() + " changed from " + describeFunctions(oldFunctions) + " to " + describeFunctions(newFunctions));

        boolean wasTherapist = Arrays.asList(oldFunctions).contains(JobFunction.THERAPIST.getValue());
        boolean stillTherapist = Arrays.asList(newFunctions).contains(JobFunction.THERAPIST.getValue());
        String warning = wasTherapist && !stillTherapist ? futureBookedAppointmentWarning(saved.getId()) : null;
        return new UserUpdateResult(userMapper.toDto(saved), warning);
    }

    private static String describeFunctions(String[] functions) {
        return functions.length == 0 ? "none" : String.join(", ", functions);
    }

    /**
     * Self-service password change for {@code PATCH /auth/password} - unlike {@link #resetPassword},
     * requires the caller to prove they know the current password. Returns the entity (not the
     * DTO) so the caller can read the bumped {@code tokenVersion} to issue a fresh token for the
     * session making this request.
     */
    @Transactional
    public UserEntity changeOwnPassword(String id, String currentPassword, String newPassword) {
        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        if (!passwordEncoder.matches(currentPassword, entity.getPasswordHash())) {
            throw new UnauthorizedException("Current password is incorrect");
        }
        entity.setPasswordHash(passwordEncoder.encode(newPassword));
        entity.setTokenVersion(entity.getTokenVersion() + 1);
        return userRepository.saveAndFlush(entity);
    }

    /**
     * Administrative reset for {@code PATCH /users/{id}/password} - ADMIN only, no current-password
     * check (this exists specifically for when the current password can't be trusted, e.g. a
     * suspected compromise). Bumps {@code tokenVersion}, immediately invalidating every token
     * already issued to this user.
     */
    @Transactional
    public User resetPassword(String id, String newPassword) {
        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        entity.setPasswordHash(passwordEncoder.encode(newPassword));
        entity.setTokenVersion(entity.getTokenVersion() + 1);
        UserEntity saved = userRepository.save(entity);
        auditLogService.record(
                AuditAction.USER_PASSWORD_RESET, AuditEntityType.USER, saved.getId(), "Password reset for " + saved.getEmail() + " by an administrator");
        return userMapper.toDto(saved);
    }

    /**
     * Enables/disables a staff account for {@code PATCH /users/{id}/active} - the closest thing
     * this stateless-JWT system has to revoking access on termination: JwtAuthFilter rejects
     * every request bearing this user's token on the very next request once disabled, regardless
     * of the token's remaining validity window. Mirrors {@link #updateRole}'s self-protection -
     * an admin can't lock themselves out by disabling their own account.
     */
    @Transactional
    public UserUpdateResult setActive(String id, String callerId, boolean active) {
        if (id.equals(callerId) && !active) {
            throw new BadRequestException("You can't disable your own account");
        }

        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        entity.setActive(active);
        entity.setTokenVersion(entity.getTokenVersion() + 1);
        UserEntity saved = userRepository.save(entity);
        auditLogService.record(
                AuditAction.USER_ACTIVE_CHANGED,
                AuditEntityType.USER,
                saved.getId(),
                "User " + saved.getEmail() + " " + (active ? "re-enabled" : "disabled"));

        String warning = !active ? futureBookedAppointmentWarning(saved.getId()) : null;
        return new UserUpdateResult(userMapper.toDto(saved), warning);
    }

    /**
     * {@code PATCH /users/{id}/overtime-eligibility} - see {@code User.overtimeEligible}'s own
     * openapi.yaml description for why this is a flag on the person rather than something
     * derived from a shift code. Like {@link #updateFunctions} and unlike
     * {@link #updateRole}/{@link #setActive}, this doesn't touch authentication at all: no
     * {@code tokenVersion} bump, no self-change restriction - there is nothing here a caller
     * could use to lock themselves out of anything.
     */
    @Transactional
    public User updateOvertimeEligible(String id, boolean overtimeEligible) {
        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        entity.setOvertimeEligible(overtimeEligible);
        UserEntity saved = userRepository.save(entity);
        auditLogService.record(
                AuditAction.USER_OVERTIME_ELIGIBILITY_CHANGED,
                AuditEntityType.USER,
                saved.getId(),
                "Overtime eligibility for " + saved.getName() + " set to " + saved.isOvertimeEligible());
        return userMapper.toDto(saved);
    }

    /**
     * {@code PATCH /users/{id}/enrollment-number} - see {@code User.enrollmentNumber}'s own
     * openapi.yaml description for what a fingerprint terminal needs this for. {@code
     * enrollmentNumber} must actually be present in the body (a number to assign, or explicit
     * {@code null} to clear) - a {@code JsonNullable} left undefined means the caller omitted the
     * field entirely, which this endpoint has no sensible no-op interpretation for. No {@code
     * tokenVersion} bump, no self-change restriction - like functions/overtimeEligible, this
     * doesn't touch authentication.
     */
    @Transactional
    public User updateEnrollmentNumber(String id, JsonNullable<Integer> enrollmentNumber) {
        if (!enrollmentNumber.isPresent()) {
            throw new BadRequestException("enrollmentNumber is required (send null to clear it)");
        }
        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        entity.setEnrollmentNumber(enrollmentNumber.get());
        UserEntity saved;
        try {
            saved = userRepository.saveAndFlush(entity);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("This enrollment number is already assigned to another employee");
        }
        auditLogService.record(
                AuditAction.USER_ENROLLMENT_NUMBER_CHANGED,
                AuditEntityType.USER,
                saved.getId(),
                "Enrollment number for " + saved.getName() + " set to "
                        + (saved.getEnrollmentNumber() != null ? saved.getEnrollmentNumber() : "none"));
        return userMapper.toDto(saved);
    }

    /**
     * Warn-don't-block (see CLAUDE.md's Failure handling section): disabling a therapist, or
     * removing THERAPIST from them, never cancels or reassigns their future appointments
     * automatically - nothing in this system cascades a staff change onto dependent records
     * today, same as a cancelled/shortened booking not cascading onto its own spa appointments
     * (see SpaAppointmentService). This only surfaces the gap so a manager notices it.
     */
    private String futureBookedAppointmentWarning(String therapistUserId) {
        List<SpaAppointmentEntity> future = spaAppointmentRepository.findByTherapistUserIdAndStatusAndDateGreaterThanEqual(
                therapistUserId, SpaAppointmentStatus.BOOKED, LocalDate.now());
        if (future.isEmpty()) {
            return null;
        }
        return "This user has " + future.size() + " upcoming spa appointment(s) still assigned to them - "
                + "they are not automatically cancelled or reassigned.";
    }
}
