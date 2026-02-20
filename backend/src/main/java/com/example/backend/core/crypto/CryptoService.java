package com.example.backend.core.crypto;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.stereotype.Service;

@Service
public class CryptoService {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_LEN_BYTES = 12;       // standard for GCM
    private static final int TAG_LEN_BITS = 128;      // 16 bytes tag

    private final SecretKey aesKey;
    private final SecureRandom random = new SecureRandom();

    public CryptoService(SecretKey aesKey) {
        this.aesKey = aesKey;
    }

    public String encryptString(String plaintext) {
        if (plaintext == null) return null;

        try {
            byte[] iv = new byte[IV_LEN_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LEN_BITS, iv));

            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));

            // Persist format: b64(iv):b64(ciphertext+tag)
            return Base64.getEncoder().encodeToString(iv) + ":" + Base64.getEncoder().encodeToString(ciphertext);
        } catch (Exception ex) {
            throw new IllegalStateException("Encryption failed", ex);
        }
    }

    public String decryptString(String stored) {
        if (stored == null) return null;

        try {
            String[] parts = stored.split(":", 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Invalid encrypted format");
            }

            byte[] iv = Base64.getDecoder().decode(parts[0]);
            byte[] ciphertext = Base64.getDecoder().decode(parts[1]);

            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, aesKey, new GCMParameterSpec(TAG_LEN_BITS, iv));

            byte[] plaintext = cipher.doFinal(ciphertext);
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new IllegalStateException("Decryption failed", ex);
        }
    }

    public String encryptBigDecimal(BigDecimal value) {
        if (value == null) return null;
        return encryptString(value.toPlainString());
    }

    public BigDecimal decryptBigDecimal(String stored) {
        if (stored == null) return null;
        return new BigDecimal(decryptString(stored));
    }
}
