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

    public String issueAccessToken(String userId, String email, String scope, Instant issuedAt, Instant expiresAt) {

        Map<String, Object> claims = new HashMap<>();
        claims.put("scope", scope);

        JwtClaimsSet claimsSet = JwtClaimsSet.builder()
                .issuer("vaultbank")
                .subject(email)          // standard: sub
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .claim("uid", userId)    // handy for DB lookup later
                .claims(c -> c.putAll(claims))
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claimsSet)).getTokenValue();
    }
}
