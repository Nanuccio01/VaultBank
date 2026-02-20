package com.example.backend.core.transfer;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers", indexes = {
        @Index(name = "ix_transfers_created_at", columnList = "created_at"),
        @Index(name = "ix_transfers_from_user", columnList = "from_user_id"),
        @Index(name = "ix_transfers_to_user", columnList = "to_user_id")
})
public class TransferEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "from_user_id", nullable = false, updatable = false)
    private UUID fromUserId;

    @Column(name = "to_user_id", updatable = false)
    private UUID toUserId; // null = bonifico esterno (solo IBAN)

    @Column(name = "from_iban", nullable = false, length = 34, updatable = false)
    private String fromIban;

    @Column(name = "to_iban", nullable = false, length = 34, updatable = false)
    private String toIban;

    @Column(name = "causal", nullable = false, length = 140, updatable = false)
    private String causal;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2, updatable = false)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public TransferEntity() {}

    public static TransferEntity create(UUID fromUserId, UUID toUserId, String fromIban, String toIban, String causal, BigDecimal amount) {
        TransferEntity t = new TransferEntity();
        t.id = UUID.randomUUID();
        t.fromUserId = fromUserId;
        t.toUserId = toUserId;
        t.fromIban = fromIban;
        t.toIban = toIban;
        t.causal = causal;
        t.amount = amount;
        t.createdAt = Instant.now();
        return t;
    }

    public UUID getId() { return id; }
    public UUID getFromUserId() { return fromUserId; }
    public UUID getToUserId() { return toUserId; }
    public String getFromIban() { return fromIban; }
    public String getToIban() { return toIban; }
    public String getCausal() { return causal; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
