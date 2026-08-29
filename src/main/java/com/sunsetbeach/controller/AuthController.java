package com.sunsetbeach.controller;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.UnauthorizedException;
import com.sunsetbeach.mapper.UserMapper;
import com.sunsetbeach.model.AuthResponse;
import com.sunsetbeach.model.ChangePasswordInput;
import com.sunsetbeach.model.LoginRequest;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserCreateInput;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.ClientIpResolver;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.LoginRateLimiter;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final LoginRateLimiter loginRateLimiter;

    public AuthController(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserService userService,
            LoginRateLimiter loginRateLimiter) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        String email = request.getEmail().trim();
        String ip = ClientIpResolver.resolve(httpRequest);
        loginRateLimiter.checkAllowed(ip, email);

        UserEntity entity = userRepository.findByEmail(email).orElse(null);
        // A disabled account fails login the same as a wrong password - no distinct error
        // message, so a caller can't use this endpoint to probe which accounts exist vs. which
        // exist but are disabled.
        if (entity == null || !entity.isActive() || !passwordEncoder.matches(request.getPassword(), entity.getPasswordHash())) {
            loginRateLimiter.recordFailure(ip, email);
            throw new UnauthorizedException("Invalid email or password");
        }
        loginRateLimiter.recordSuccess(ip, email);

        StaffPrincipal principal = new StaffPrincipal(entity.getId(), entity.getEmail(), entity.getRole());
        String token = jwtService.issue(principal, entity.getTokenVersion());
        return ResponseEntity.ok(new AuthResponse(token, userMapper.toDto(entity)));
    }

    /**
     * Self-service password change - the only account-security action available without ADMIN
     * (see {@code PATCH /users/{id}/password} for the admin-reset counterpart). Bumps
     * {@code tokenVersion}, which invalidates every other token already issued to this user
     * (other devices/tabs, or a token that leaked) on their very next request - the response
     * carries a fresh token so this session keeps working without a re-login.
     */
    @PatchMapping("/auth/password")
    public ResponseEntity<AuthResponse> changeOwnPassword(@Valid @RequestBody ChangePasswordInput input) {
        StaffPrincipal principal = currentPrincipal();
        UserEntity updated = userService.changeOwnPassword(principal.id(), input.getCurrentPassword(), input.getNewPassword());
        StaffPrincipal refreshed = new StaffPrincipal(updated.getId(), updated.getEmail(), updated.getRole());
        String token = jwtService.issue(refreshed, updated.getTokenVersion());
        return ResponseEntity.ok(new AuthResponse(token, userMapper.toDto(updated)));
    }

    private static StaffPrincipal currentPrincipal() {
        return (StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    /**
     * Same behavior as POST /users - kept as an alias under /auth for API consumers that
     * expect a conventional auth/register route. Requires an ADMIN session (see SecurityConfig).
     */
    @PostMapping("/auth/register")
    public ResponseEntity<User> register(@Valid @RequestBody UserCreateInput input) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.create(input));
    }

    @GetMapping("/auth/me")
    public ResponseEntity<User> me() {
        StaffPrincipal principal = currentPrincipal();
        UserEntity entity = userRepository.findById(principal.id())
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        return ResponseEntity.ok(userMapper.toDto(entity));
    }

    @PostMapping("/auth/logout")
    public ResponseEntity<Void> logout() {
        // Stateless JWT, nothing to revoke server-side - the client drops the token.
        return ResponseEntity.noContent().build();
    }
}
