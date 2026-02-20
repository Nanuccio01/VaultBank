package com.example.backend.core.transfer;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<TransferEntity, UUID> {

    @Query("select t from TransferEntity t " +
            "where t.fromUserId = :uid or t.toUserId = :uid " +
            "order by t.createdAt desc")
    List<TransferEntity> findLatestMovements(@Param("uid") UUID uid, Pageable pageable);
}
