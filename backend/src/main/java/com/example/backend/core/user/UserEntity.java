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

    @Column(name = "first_name_enc", length = 2048)
    private String firstNameEnc;

    @Column(name = "last_name_enc", length = 2048)
    private String lastNameEnc;

    @Column(name = "phone_enc", length = 2048)
    private String phoneEnc;

    @Column(name = "balance_enc", length = 2048)
    private String balanceEnc;

    @Column(name = "iban", length = 34)
    private String iban;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

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

    public String getFirstNameEnc() { return firstNameEnc; }
    public void setFirstNameEnc(String firstNameEnc) { this.firstNameEnc = firstNameEnc; }

    public String getLastNameEnc() { return lastNameEnc; }
    public void setLastNameEnc(String lastNameEnc) { this.lastNameEnc = lastNameEnc; }

    public String getPhoneEnc() { return phoneEnc; }
    public void setPhoneEnc(String phoneEnc) { this.phoneEnc = phoneEnc; }

    public String getBalanceEnc() { return balanceEnc; }
    public void setBalanceEnc(String balanceEnc) { this.balanceEnc = balanceEnc; }

    public String getIban() { return iban; }
    public void setIban(String iban) { this.iban = iban; }
}