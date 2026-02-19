package com.example.backend.core.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final InMemoryUserStore store = new InMemoryUserStore();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final JwtTokenService jwtTokenService;
    private final long ttlMin;

    public AuthService(JwtTokenService jwtTokenService,
                       @Value("${vaultbank.jwt.ttl-min:30}") long ttlMin) {
        this.jwtTokenService = jwtTokenService;
        this.ttlMin = ttlMin;
    }

    public void register(String email, String rawPassword) {
        if (store.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        String hash = passwordEncoder.encode(rawPassword);

        // For demo: default scopes include read+write
        String scope = "read write";

        store.save(new InMemoryUserStore.UserRecord(
                UUID.randomUUID(),
                email,
                hash,
                Instant.now(),
                scope
        ));
    }

    public TokenResult login(String email, String rawPassword) {
        InMemoryUserStore.UserRecord user = store.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.passwordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        Instant now = Instant.now();
        Instant exp = now.plus(ttlMin, ChronoUnit.MINUTES);

        String token = jwtTokenService.issueAccessToken(user.id().toString(), user.email(), user.scope(), now, exp);
        long expiresSec = ChronoUnit.SECONDS.between(now, exp);

        return new TokenResult(token, expiresSec, user.scope());
    }

    public record TokenResult(String token, long expiresInSeconds, String scope) {}
}
