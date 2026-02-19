package com.example.backend.api.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class AuthDtos {

    private AuthDtos() { }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Email @NotBlank @Size(max = 200) String email,
            @NotBlank @Pattern(regexp = "^[0-9+ ]{7,20}$", message = "Phone must contain digits and optional +/spaces") String phone,
            @NotBlank @Size(min = 8, max = 200) String password
    ) {}

    public record LoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}

    public record TokenResponse(
            String accessToken,
            String tokenType,
            long expiresInSeconds,
            String scope
    ) {}
}
