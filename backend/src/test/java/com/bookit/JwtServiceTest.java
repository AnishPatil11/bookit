package com.bookit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.bookit.config.AppProperties;
import com.bookit.domain.Role;
import com.bookit.domain.User;
import com.bookit.security.JwtService;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private static AppProperties props(String secret) {
        return new AppProperties("UTC", new AppProperties.Jwt(secret, 60), new AppProperties.Cors("*"),
                new AppProperties.Reservation(90, 30, 2, 60), new AppProperties.Seed("a@b.c", "x", false));
    }

    private static User user() {
        User u = new User("Asha", "asha@test.dev", "hash", Role.CUSTOMER, LocalDateTime.now());
        ReflectionTestUtils.setField(u, "id", 42L);
        return u;
    }

    @Test
    void roundTripReturnsUserId() {
        JwtService jwt = new JwtService(props("0123456789-0123456789-0123456789-01"), Clock.systemUTC());
        String token = jwt.generate(user());
        assertEquals(42L, jwt.parseUserId(token).orElseThrow());
    }

    @Test
    void tamperedTokenIsRejected() {
        JwtService jwt = new JwtService(props("0123456789-0123456789-0123456789-01"), Clock.systemUTC());
        String token = jwt.generate(user());
        assertTrue(jwt.parseUserId(token + "x").isEmpty());
        assertTrue(jwt.parseUserId("not-a-jwt").isEmpty());
    }

    @Test
    void expiredTokenIsRejected() {
        String secret = "0123456789-0123456789-0123456789-01";
        Clock past = Clock.fixed(Instant.now().minusSeconds(3 * 3600), ZoneOffset.UTC);
        String token = new JwtService(props(secret), past).generate(user());
        assertTrue(new JwtService(props(secret), Clock.systemUTC()).parseUserId(token).isEmpty());
    }

    @Test
    void shortSecretFailsFast() {
        assertThrows(IllegalStateException.class, () -> new JwtService(props("too-short"), Clock.systemUTC()));
    }
}
