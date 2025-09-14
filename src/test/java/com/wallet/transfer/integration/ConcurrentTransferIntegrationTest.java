package com.wallet.transfer.integration;

import com.wallet.transfer.dto.TransferRequestDTO;
import com.wallet.transfer.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@SpringBootTest
public class ConcurrentTransferIntegrationTest {
    @Autowired
    private TransferService transferService;

    @Test
    void concurrentTransfers() throws InterruptedException {
        TransferRequestDTO request = new TransferRequestDTO();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.TEN);

        String idempontencyKey = "unique-key-123";
        int threads = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads);
        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    transferService.transfer(request,idempontencyKey);
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
        executor.shutdown();
        // Add assertions for correctness as needed
    }
}
