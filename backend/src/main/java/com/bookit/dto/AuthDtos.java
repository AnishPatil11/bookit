package com.bookit.dto;

import com.bookit.domain.Role;
import com.bookit.domain.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class AuthDtos {
    private AuthDtos() {}

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String name,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 72, message = "must be 8 to 72 characters") String password) {}

    public record LoginRequest(@NotBlank String email, @NotBlank String password) {}

    public record UserResponse(Long id, String name, String email, Role role) {
        public static UserResponse from(User u) {
            return new UserResponse(u.getId(), u.getName(), u.getEmail(), u.getRole());
        }
    }

    public record AuthResponse(String token, UserResponse user) {}
}
