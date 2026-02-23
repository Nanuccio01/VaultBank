package com.example.backend.api.auth;

import com.example.backend.core.auth.JwtTokenService;
import com.example.backend.core.fraud.FraudService;
import com.example.backend.core.user.UserEntity;
import com.example.backend.core.user.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class StepUpController {

    private final UserRepository userRepository;
    private final JwtTokenService jwtTokenService;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final long ttlMin;
    private final FraudService fraudService;

    public StepUpController(UserRepository userRepository,
                            JwtTokenService jwtTokenService,
                            FraudService fraudService,
                            @Value("${vaultbank.jwt.stepup-ttl-min:5}") long ttlMin) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.fraudService = fraudService;
        this.ttlMin = ttlMin;
    }

    public record StepUpRequest(@NotBlank String password) {}
    public record StepUpResponse(String accessToken, String tokenType, long expiresInSeconds) {}

    @PostMapping("/stepup")
    public StepUpResponse stepUp(@Valid @RequestBody StepUpRequest req, Authentication auth) {
        Jwt jwt = (Jwt) auth.getPrincipal();
        UUID uid = UUID.fromString(jwt.getClaimAsString("uid"));
        Instant now = Instant.now();

        // se lockato -> blocco temporaneo
        FraudService.LockStatus ls = fraudService.checkAndClearLock(uid, now);
        if (ls.locked()) {
            throw new com.example.backend.api.banking.FraudExceptions.FraudBlockedException(
                    List.of("Account temporarily locked"),
                    ls.lockedUntil()
            );
        }

        UserEntity user = userRepository.findById(uid)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // password errata -> applica lock 3 min
        if (!encoder.matches(req.password(), user.getPasswordHash())) {
            Instant until = fraudService.applyTemporaryLock(uid, now, List.of("Step-up password failed"));
            throw new com.example.backend.api.banking.FraudExceptions.FraudBlockedException(
                    List.of("Step-up password failed"),
                    until
            );
        }

        // password corretta -> emetti token step-up
        Instant exp = now.plus(ttlMin, ChronoUnit.MINUTES);
        String scope = "read write";

        String token = jwtTokenService.issueAccessToken(
                user.getId().toString(),
                user.getEmail(),
                scope,
                now,
                exp,
                true
        );

        long expires = ChronoUnit.SECONDS.between(now, exp);
        return new StepUpResponse(token, "Bearer", expires);
    }
}
