package com.example.backend.core.transfer;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<TransferEntity, UUID> {
    List<TransferEntity> findTop10ByFromUserIdOrderByCreatedAtDesc(UUID fromUserId);
}
