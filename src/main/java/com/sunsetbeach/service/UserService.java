package com.sunsetbeach.service;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.BadRequestException;
import com.sunsetbeach.error.ConflictException;
import com.sunsetbeach.error.NotFoundException;
import com.sunsetbeach.error.UnauthorizedException;
import com.sunsetbeach.mapper.UserMapper;
import com.sunsetbeach.model.Role;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserCreateInput;
import com.sunsetbeach.model.UserRoleUpdateInput;
import com.sunsetbeach.repository.UserRepository;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<User> list() {
        return userRepository.findAll().stream().map(userMapper::toDto).toList();
    }

    @Transactional(readOnly = true)
    public User getById(String id) {
        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        return userMapper.toDto(entity);
    }

    @Transactional
    public User create(UserCreateInput input) {
        UserEntity entity = new UserEntity();
        entity.setEmail(input.getEmail().trim());
        entity.setPasswordHash(passwordEncoder.encode(input.getPassword()));
        entity.setRole(input.getRole() != null ? input.getRole() : Role.MANAGER);

        try {
            return userMapper.toDto(userRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("A user with that email already exists");
        }
    }

    @Transactional
    public User updateRole(String id, String callerId, UserRoleUpdateInput input) {
        if (id.equals(callerId)) {
            throw new BadRequestException("You can't change your own role");
        }

        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        entity.setRole(input.getRole());
        // A token issued before this change still carries the old role in its own "role" claim
        // (JwtAuthFilter builds authorities from the token, not a fresh DB read) - bumping
        // tokenVersion is what forces that token to fail verification on its next request so a
        // re-login is required to pick up the new role.
        entity.setTokenVersion(entity.getTokenVersion() + 1);
        return userMapper.toDto(userRepository.save(entity));
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
        return userMapper.toDto(userRepository.save(entity));
    }

    /**
     * Enables/disables a staff account for {@code PATCH /users/{id}/active} - the closest thing
     * this stateless-JWT system has to revoking access on termination: JwtAuthFilter rejects
     * every request bearing this user's token on the very next request once disabled, regardless
     * of the token's remaining validity window. Mirrors {@link #updateRole}'s self-protection -
     * an admin can't lock themselves out by disabling their own account.
     */
    @Transactional
    public User setActive(String id, String callerId, boolean active) {
        if (id.equals(callerId) && !active) {
            throw new BadRequestException("You can't disable your own account");
        }

        UserEntity entity = userRepository.findById(id).orElseThrow(() -> new NotFoundException("User not found"));
        entity.setActive(active);
        entity.setTokenVersion(entity.getTokenVersion() + 1);
        return userMapper.toDto(userRepository.save(entity));
    }
}
