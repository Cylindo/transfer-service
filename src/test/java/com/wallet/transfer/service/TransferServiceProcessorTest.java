package com.wallet.transfer.service;


import com.wallet.transfer.dto.TransferRequestDTO;
import com.wallet.transfer.dto.TransferResultDTO;
import com.wallet.transfer.entity.Transfer;
import com.wallet.transfer.repository.TransferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransferServiceProcessorTest {
    @Mock
    private TransferRepository transferRepository;
    @Mock
    private RestTemplate restTemplate;
    @InjectMocks
    private TransferServiceProcessor processor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new TransferServiceProcessor(restTemplate, transferRepository, "http://api", "/transfer");
    }

    @Test
    void processTransfer_success() {
        TransferRequestDTO request = new TransferRequestDTO();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.TEN);

        TransferResultDTO ledgerResult = new TransferResultDTO();
        ledgerResult.setStatus("success");

        when(restTemplate.postForObject(any(URI.class), any(HttpEntity.class), eq(TransferResultDTO.class)))
                .thenReturn(ledgerResult);

        Transfer saved = new Transfer();
        saved.setTransferId("T1");
        saved.setStatus("success");
        when(transferRepository.save(any(Transfer.class))).thenReturn(saved);

        TransferResultDTO result = processor.processTransfer(request);

        assertEquals("success", result.getStatus());
    }

    @Test
    void processTransfer_idempotent_existing() {
        TransferRequestDTO request = new TransferRequestDTO();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.TEN);

        String idempotencyKey = "IDEMPOTENT-KEY";
        Transfer existing = new Transfer();
        existing.setTransferId(idempotencyKey);
        existing.setStatus("success");

        when(transferRepository.findByTransferId(idempotencyKey)).thenReturn(Optional.of(existing));

        TransferResultDTO result = processor.processTransfer(request, idempotencyKey);

        assertEquals("success", result.getStatus());
        assertEquals(idempotencyKey, result.getTransferId());
        verify(restTemplate, never()).postForObject(any(), any(), eq(TransferResultDTO.class));
    }

    @Test
    void getTransferById_success() {
        String transferId = "T123";
        Transfer transfer = new Transfer();
        transfer.setTransferId(transferId);
        transfer.setStatus("success");

        when(transferRepository.findByTransferId(transferId)).thenReturn(Optional.of(transfer));

        Transfer result = processor.getTransferById(transferId);

        assertEquals(transferId, result.getTransferId());
        assertEquals("success", result.getStatus());
    }

    @Test
    void getTransferById_notFound() {
        String transferId = "NOT_FOUND";
        when(transferRepository.findByTransferId(transferId)).thenReturn(Optional.empty());

        IllegalArgumentException ex = org.junit.jupiter.api.Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> processor.getTransferById(transferId)
        );
        assertEquals("Transfer not found with ID: " + transferId, ex.getMessage());
    }
}
