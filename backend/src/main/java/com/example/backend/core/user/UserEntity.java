package com.example.backend.core.user;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(name = "uk_users_email", columnNames = "email"))
public class UserEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(nullable = false, length = 200)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 200)
    private String passwordHash;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // Placeholder per prossimi step (AES + banking)
    @Column(name = "iban", length = 34)
    private String iban;

    public UserEntity() {}

    public static UserEntity create(String email, String passwordHash) {
        UserEntity u = new UserEntity();
        u.id = UUID.randomUUID();
        u.email = email.toLowerCase();
        u.passwordHash = passwordHash;
        u.createdAt = Instant.now();
        return u;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public Instant getCreatedAt() { return createdAt; }
    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
}
