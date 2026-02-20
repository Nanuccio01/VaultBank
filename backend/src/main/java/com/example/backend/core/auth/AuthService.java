package com.example.backend.core.auth;

import com.example.backend.core.banking.IbanGenerator;
import com.example.backend.core.crypto.CryptoService;
import com.example.backend.core.user.UserEntity;
import com.example.backend.core.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtTokenService jwtTokenService;
    private final CryptoService cryptoService;
    private final IbanGenerator ibanGenerator;
    private final long ttlMin;

    public AuthService(UserRepository userRepository,
                       JwtTokenService jwtTokenService,
                       CryptoService cryptoService,
                       IbanGenerator ibanGenerator,
                       @Value("${vaultbank.jwt.ttl-min:30}") long ttlMin) {
        this.userRepository = userRepository;
        this.jwtTokenService = jwtTokenService;
        this.cryptoService = cryptoService;
        this.ibanGenerator = ibanGenerator;
        this.ttlMin = ttlMin;
    }

    public void register(String email, String rawPassword, String firstName, String lastName, String phone) {
        if (userRepository.existsByEmail(email.toLowerCase())) {
            throw new IllegalArgumentException("Email already registered");
        }

        String hash = passwordEncoder.encode(rawPassword);

        UserEntity user = UserEntity.create(email, hash);

        user.setFirstNameEnc(cryptoService.encryptString(firstName));
        user.setLastNameEnc(cryptoService.encryptString(lastName));
        user.setPhoneEnc(cryptoService.encryptString(phone));

        user.setIban(ibanGenerator.generateItalianIban());

        BigDecimal initialBalance = new BigDecimal("1000.00");
        user.setBalanceEnc(cryptoService.encryptBigDecimal(initialBalance));

        userRepository.save(user);
    }

    public TokenResult login(String email, String rawPassword) {
        UserEntity user = userRepository.findByEmail(email.toLowerCase())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String scope = "read write";
        Instant now = Instant.now();
        Instant exp = now.plus(ttlMin, ChronoUnit.MINUTES);

        String token = jwtTokenService.issueAccessToken(user.getId().toString(), user.getEmail(), scope, now, exp);
        long expiresSec = ChronoUnit.SECONDS.between(now, exp);

        return new TokenResult(token, expiresSec, scope);
    }

    public record TokenResult(String token, long expiresInSeconds, String scope) {}
}