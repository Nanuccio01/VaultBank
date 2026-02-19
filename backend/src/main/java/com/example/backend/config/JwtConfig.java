package com.example.backend.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;

@Configuration
public class JwtConfig {

    @Bean
    public SecretKey jwtHmacKey(@Value("${vaultbank.jwt.hs256-secret-b64}") String secretB64) {
        byte[] secret = Base64.getDecoder().decode(secretB64);
        return new SecretKeySpec(secret, "HmacSHA256");
    }

    @Bean
    public JwtDecoder jwtDecoder(SecretKey jwtHmacKey) {
        return NimbusJwtDecoder
                .withSecretKey(jwtHmacKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    public JwtEncoder jwtEncoder(SecretKey jwtHmacKey) {
        ImmutableSecret<SecurityContext> secret = new ImmutableSecret<>(jwtHmacKey);
        return new NimbusJwtEncoder(secret);
    }
}