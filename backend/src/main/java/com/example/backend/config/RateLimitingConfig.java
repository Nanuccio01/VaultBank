package com.example.backend.config;

import io.github.resilience4j.ratelimiter.RateLimiter;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class RateLimitingConfig {

    @Bean
    public RateLimiter loginRateLimiter() {
        RateLimiterConfig cfg = RateLimiterConfig.custom()
                // Esempio demo: max 5 tentativi ogni 30 secondi
                .limitForPeriod(5)
                .limitRefreshPeriod(Duration.ofSeconds(30))
                .timeoutDuration(Duration.ZERO) // se pieno -> rifiuta subito
                .build();

        return RateLimiter.of("login", cfg);
    }
}
