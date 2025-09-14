package com.wallet.transfer.repository;

import com.wallet.transfer.entity.Transfer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TransferRepository extends JpaRepository<Transfer, Long> {
    Optional<Transfer> findByTransferId(String transferId);

    @Modifying
    @Query("DELETE FROM Transfer t WHERE t.createdAt < :cutoff")
    int deleteByCreatedAtBefore(LocalDateTime cutoff);
}
