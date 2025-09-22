package com.wallet.transfer.integration;

import com.wallet.transfer.dto.TransferRequestDTO;
import com.wallet.transfer.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "ledger-api.host=http://ledger-service:59999", // unreachable to force fallback quickly
        "ledger-api.transfer.api=/api/ledger/transfer"
})
public class ConcurrentTransferIntegrationTest {
    @Autowired
    private TransferService transferService;

    @Test
    void concurrentTransfers() throws InterruptedException {
        TransferRequestDTO request = new TransferRequestDTO();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.TEN);

        String idempotencyKey = "concurrent-key"; // same key to exercise idempotency under contention
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    transferService.transfer(request, idempotencyKey);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();
        // Potential extension: verify only one persisted transfer when repository accessible.
    }
}
