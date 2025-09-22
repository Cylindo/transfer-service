package com.wallet.transfer.integration;

import com.wallet.transfer.dto.TransferRequestDTO;
import com.wallet.transfer.dto.TransferResultDTO;
import com.wallet.transfer.service.TransferService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "ledger-api.host=http://ledger-service:59999", // unreachable to force circuit breaker fallback
        "ledger-api.transfer.api=/api/ledger/transfer"
})
class CircuitBreakerIntegrationTest {

    @Autowired
    private TransferService transferService;

    @Test
    void circuitBreakerFallback_onLedgerFailure() {
        TransferRequestDTO request = new TransferRequestDTO();
        request.setFromAccountId(999L); // arbitrary id to simulate failure path
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.ONE);

        TransferResultDTO result = transferService.transfer(request, "cb-fail-1");
        assertNotNull(result);
        assertEquals("failure", result.getStatus(), "Expected fallback status 'failure'");
    }
}
