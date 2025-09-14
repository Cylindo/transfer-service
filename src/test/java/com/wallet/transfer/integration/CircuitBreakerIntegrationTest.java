package com.wallet.transfer.integration;

import com.wallet.transfer.dto.TransferRequestDTO;
import com.wallet.transfer.entity.Transfer;
import com.wallet.transfer.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CircuitBreakerIntegrationTest {
    @Autowired
    private TransferService transferService;

    @Test
    @CircuitBreaker(name = "accountService", fallbackMethod = "fallback")
    void transferWithFailure() {
        TransferRequestDTO request = new TransferRequestDTO();
        request.setFromAccountId(999L); // Non-existent account to simulate failure
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.ONE);

        // Simulate failure (throwing exception in AccountService call)
        assertDoesNotThrow(() -> transferService.transfer(request, "test-user"));
    }

    public Transfer fallback(Long fromAccountId, Long toAccountId, BigDecimal amount, Throwable t) {
        Transfer transfer = new Transfer();
        transfer.setStatus("FAILED");
        return transfer;
    }
}
