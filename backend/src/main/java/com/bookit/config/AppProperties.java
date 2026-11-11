package com.bookit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the "app.*" block in application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String zone, Jwt jwt, Cors cors, Reservation reservation, Seed seed) {

    public record Jwt(String secret, int expirationMinutes) {}

    public record Cors(String allowedOrigins) {}

    public record Reservation(int slotMinutes, int slotStepMinutes, int cancelCutoffHours, int maxDaysAhead) {}

    public record Seed(String adminEmail, String adminPassword, boolean demoData) {}
}
