package com.example.backend.config;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CryptoConfig {

    @Bean
    public SecretKey aesKey(@Value("${vaultbank.crypto.aes-key-b64}") String aesKeyB64) {
        byte[] key = Base64.getDecoder().decode(aesKeyB64);

        // Requirement: 32 bytes (AES-256)
        if (key.length != 32) {
            throw new IllegalStateException("AES key must be 32 bytes (base64 of 32 bytes). Current length: " + key.length);
        }

        return new SecretKeySpec(key, "AES");
    }
}
