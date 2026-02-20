package com.example.backend.api.auth;

import com.example.backend.api.auth.AuthDtos.LoginRequest;
import com.example.backend.api.auth.AuthDtos.RegisterRequest;
import com.example.backend.api.auth.AuthDtos.TokenResponse;
import com.example.backend.core.auth.AuthService;
import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RequestNotPermitted;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final RateLimiter loginRateLimiter;

    public AuthController(AuthService authService, RateLimiter loginRateLimiter) {
        this.authService = authService;
        this.loginRateLimiter = loginRateLimiter;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req.email(), req.password(), req.firstName(), req.lastName(), req.phone());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest req) {
        try {
            AuthService.TokenResult result = RateLimiter
                    .decorateSupplier(loginRateLimiter, () -> authService.login(req.email(), req.password()))
                    .get();

            return ResponseEntity.ok(new TokenResponse(
                    result.token(),
                    "Bearer",
                    result.expiresInSeconds(),
                    result.scope()
            ));
        } catch (RequestNotPermitted ex) {
            throw new TooManyRequestsException("Too many login attempts. Please retry later.");
        }
    }
}
