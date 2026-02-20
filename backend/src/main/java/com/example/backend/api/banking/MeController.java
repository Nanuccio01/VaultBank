package com.example.backend.api.banking;

import com.example.backend.core.crypto.CryptoService;
import com.example.backend.core.user.UserEntity;
import com.example.backend.core.user.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/banking")
public class MeController {

    private final UserRepository userRepository;
    private final CryptoService cryptoService;

    public MeController(UserRepository userRepository, CryptoService cryptoService) {
        this.userRepository = userRepository;
        this.cryptoService = cryptoService;
    }

    public record MeResponse(
            String email,
            String firstName,
            String lastName,
            String phone,
            String iban,
            BigDecimal balance
    ) {}

    @GetMapping("/me")
    public MeResponse me(Authentication authentication) {
        Jwt jwt = (Jwt) authentication.getPrincipal();

        String uid = jwt.getClaimAsString("uid");
        UserEntity user = userRepository.findById(UUID.fromString(uid))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return new MeResponse(
                user.getEmail(),
                cryptoService.decryptString(user.getFirstNameEnc()),
                cryptoService.decryptString(user.getLastNameEnc()),
                cryptoService.decryptString(user.getPhoneEnc()),
                user.getIban(),
                cryptoService.decryptBigDecimal(user.getBalanceEnc())
        );
    }
}