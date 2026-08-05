package com.sunsetbeach.controller;

import com.sunsetbeach.entity.UserEntity;
import com.sunsetbeach.error.UnauthorizedException;
import com.sunsetbeach.mapper.UserMapper;
import com.sunsetbeach.model.AuthResponse;
import com.sunsetbeach.model.LoginRequest;
import com.sunsetbeach.model.User;
import com.sunsetbeach.model.UserCreateInput;
import com.sunsetbeach.repository.UserRepository;
import com.sunsetbeach.security.JwtService;
import com.sunsetbeach.security.StaffPrincipal;
import com.sunsetbeach.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
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

    public AuthController(
            UserRepository userRepository,
            UserMapper userMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            UserService userService) {
        this.userRepository = userRepository;
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
    }

    @PostMapping("/auth/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        UserEntity entity = userRepository.findByEmail(request.getEmail().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!passwordEncoder.matches(request.getPassword(), entity.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        StaffPrincipal principal = new StaffPrincipal(entity.getId(), entity.getEmail(), entity.getRole());
        String token = jwtService.issue(principal);
        return ResponseEntity.ok(new AuthResponse(token, userMapper.toDto(entity)));
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
        StaffPrincipal principal = (StaffPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
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
