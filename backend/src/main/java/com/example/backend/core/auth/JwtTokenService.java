package com.example.backend.core.auth;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;

    public JwtTokenService(JwtEncoder jwtEncoder) {
        this.jwtEncoder = jwtEncoder;
    }

    public String issueAccessToken(String userId, String email, String scope, Instant issuedAt, Instant expiresAt, boolean stepUp) {

        JwtClaimsSet.Builder b = JwtClaimsSet.builder()
                .issuer("vaultbank")
                .subject(email)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("uid", userId)
                .claim("scope", scope);

        if (stepUp) {
            b.claim("stepup", true);
        }

        JwtClaimsSet claimsSet = b.build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claimsSet)).getTokenValue();
    }
}
