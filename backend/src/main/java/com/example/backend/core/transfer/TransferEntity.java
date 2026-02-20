package com.example.backend.core.transfer;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "ix_transfers_from_user_created_at", columnList = "from_user_id, created_at")
})
public class TransferEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "from_user_id", nullable = false, updatable = false)
    private UUID fromUserId;

    @Column(name = "to_iban", nullable = false, length = 34, updatable = false)
    private String toIban;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TransferEntity() {}

    public static TransferEntity create(UUID fromUserId, String toIban, BigDecimal amount) {
        TransferEntity t = new TransferEntity();
        t.id = UUID.randomUUID();
        t.fromUserId = fromUserId;
        t.toIban = toIban;
        t.amount = amount;
        t.createdAt = Instant.now();
        return t;
    }

    public UUID getId() { return id; }
    public UUID getFromUserId() { return fromUserId; }
    public String getToIban() { return toIban; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
