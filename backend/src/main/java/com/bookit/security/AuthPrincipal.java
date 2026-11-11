package com.bookit.security;

import com.bookit.domain.Role;

/** The logged-in user as seen by controllers (@AuthenticationPrincipal). */
public record AuthPrincipal(Long id, String email, Role role) {
    public boolean isAdmin() { return role == Role.ADMIN; }
}
