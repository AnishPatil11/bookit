package com.bookit.service;

import com.bookit.domain.Role;
import com.bookit.domain.User;
import com.bookit.dto.AuthDtos.AuthResponse;
import com.bookit.dto.AuthDtos.LoginRequest;
import com.bookit.dto.AuthDtos.RegisterRequest;
import com.bookit.dto.AuthDtos.UserResponse;
import com.bookit.exception.ApiException;
import com.bookit.repository.UserRepository;
import com.bookit.security.AuthPrincipal;
import com.bookit.security.JwtService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final Clock clock;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, Clock clock) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.clock = clock;
    }

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("An account with this email already exists");
        }
        User user = users.save(new User(req.name().trim(), email, encoder.encode(req.password()),
                Role.CUSTOMER, LocalDateTime.now(clock)));
        return new AuthResponse(jwt.generate(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(req.email().trim())
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        return new AuthResponse(jwt.generate(user), UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public UserResponse me(AuthPrincipal principal) {
        return users.findById(principal.id()).map(UserResponse::from)
                .orElseThrow(() -> ApiException.unauthorized("Account no longer exists"));
    }
}
