package com.example.backend.core.auth;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryUserStore {

    public record UserRecord(
            UUID id,
            String email,
            String passwordHash,
            Instant createdAt,
            String scope
    ) {}

    private final Map<String, UserRecord> byEmail = new ConcurrentHashMap<>();

    public Optional<UserRecord> findByEmail(String email) {
        return Optional.ofNullable(byEmail.get(email.toLowerCase()));
    }

    public UserRecord save(UserRecord user) {
        byEmail.put(user.email().toLowerCase(), user);
        return user;
    }

    public boolean existsByEmail(String email) {
        return byEmail.containsKey(email.toLowerCase());
    }
}
