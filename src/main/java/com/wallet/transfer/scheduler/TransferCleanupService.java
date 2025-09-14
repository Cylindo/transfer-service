package com.wallet.transfer.scheduler;

import com.wallet.transfer.repository.TransferRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransferCleanupService {

    private final TransferRepository transferRepository;

    // Runs every hour
    @Scheduled(fixedRate = 60 * 60 * 1000)
    @Transactional
    public void cleanupOldTransfers() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(24);
        int deleted = transferRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Deleted {} transfer(s) older than 24 hours", deleted);
    }
}
