package com.wallet.transfer.service;

import com.wallet.transfer.dto.TransferRequestDTO;
import com.wallet.transfer.dto.TransferResultDTO;
import com.wallet.transfer.entity.Transfer;
import com.wallet.transfer.repository.TransferRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Optional;
import org.slf4j.MDC;

@Slf4j
@Component
public class TransferServiceProcessor {
    private final TransferRepository transferRepository;
    private final RestTemplate restTemplate;
    private final String apiHostContext;
    private final String ledgerTransferApi;

    public TransferServiceProcessor(RestTemplate restTemplate,TransferRepository transferRepository,
                                    @Value("${ledger-api.host}") String apiHostContext,
                                    @Value("${ledger-api.transfer.api}") String ledgerTransferApi) {
        this.restTemplate = restTemplate;
        this.transferRepository = transferRepository;
        this.apiHostContext = apiHostContext;
        this.ledgerTransferApi = ledgerTransferApi;
    }
    /**
     * Process a transfer between two accounts.
     *
     * @param request The transfer request data.
     * @return The result of the transfer operation.
     */
    @Transactional
    public TransferResultDTO processTransfer(TransferRequestDTO request) {
        log.info("Processing transfer from account {} to account {} for amount {}", request.getFromAccountId(), request.getToAccountId(), request.getAmount());
        // 1. Input validation
        if (request.getFromAccountId() == null || request.getToAccountId() == null || request.getAmount() == null) {
            throw new IllegalArgumentException("Invalid transfer request: missing required fields");
        }
        // 2. Call Ledger Service for atomic debit/credit
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        HttpEntity<TransferRequestDTO> entity = new HttpEntity<>(request, requestHeaders);
        URI uri = UriComponentsBuilder.fromHttpUrl(apiHostContext + ledgerTransferApi)
                .build()
                .toUri();
        TransferResultDTO ledgerResult = restTemplate.postForObject(uri, entity, TransferResultDTO.class);
        // Persist the transfer result
        Transfer transfer = new Transfer();
        transfer.setFromAccountId(request.getFromAccountId());
        transfer.setToAccountId(request.getToAccountId());
        transfer.setAmount(request.getAmount());
        transfer.setStatus(ledgerResult.getStatus());
        Transfer saved = transferRepository.save(transfer);
        // Build result DTO
        TransferResultDTO result = new TransferResultDTO();
        result.setTransferId(saved.getTransferId() != null ? saved.getTransferId() : null);
        result.setStatus(saved.getStatus());
        return result;
    }

    /**
     * Process a transfer between two accounts with idempotency.
     *
     * @param request         The transfer request data.
     * @param idempotencyKey  The idempotency key to ensure the request is processed only once.
     * @return The result of the transfer operation.
     */
    @Transactional
    @CircuitBreaker(name = "ledgerService", fallbackMethod = "ledgerServiceFallback")
    public TransferResultDTO processTransfer(TransferRequestDTO request, String idempotencyKey) {
        String correlationId = MDC.get("correlationId");
        log.info("[correlationId={}] Processing transfer from account {} to account {} for amount {} with idempotencyKey {}", correlationId, request.getFromAccountId(), request.getToAccountId(), request.getAmount(), idempotencyKey);
        // 1. Input validation
        if (request.getFromAccountId() == null || request.getToAccountId() == null || request.getAmount() == null) {
            throw new IllegalArgumentException("Invalid transfer request: missing required fields");
        }

        // Check for existing transfer by transferId/idempotencyKey
        Optional<Transfer> existing = transferRepository.findByTransferId(idempotencyKey);
        if (existing.isPresent()) {
            Transfer existingTransfer = existing.get();
            log.info("[correlationId={}] Duplicate transfer detected for idempotencyKey {}. Returning existing result.", correlationId, idempotencyKey);
            TransferResultDTO result = new TransferResultDTO();
            result.setTransferId(existingTransfer.getTransferId());
            result.setStatus(existingTransfer.getStatus());
            return result;
        }
        // Call Ledger Service for atomic debit/credit (circuit breaker applied)
        HttpHeaders requestHeaders = new HttpHeaders();
        requestHeaders.setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
        requestHeaders.set("Idempotency-Key", idempotencyKey); // set your key here

        HttpEntity<TransferRequestDTO> entity = new HttpEntity<>(request, requestHeaders);
        URI uri = UriComponentsBuilder.fromHttpUrl(apiHostContext + ledgerTransferApi)
                .build()
                .toUri();
        TransferResultDTO ledgerResult = restTemplate.postForObject(uri, entity, TransferResultDTO.class);
        // Persist the transfer result
        Transfer transfer = new Transfer();
        transfer.setTransferId(idempotencyKey);
        transfer.setFromAccountId(request.getFromAccountId());
        transfer.setToAccountId(request.getToAccountId());
        transfer.setAmount(request.getAmount());
        transfer.setStatus(ledgerResult != null ? ledgerResult.getStatus() : "failure");
        Transfer saved = transferRepository.save(transfer);
        // Build result DTO
        TransferResultDTO result = new TransferResultDTO();
        result.setTransferId(saved.getTransferId());
        result.setStatus(saved.getStatus());
        return result;
    }

    // Fallback method for circuit breaker
    public TransferResultDTO ledgerServiceFallback(TransferRequestDTO request, String idempotencyKey, Throwable t) {
        String correlationId = MDC.get("correlationId");
        log.error("[correlationId={}] Ledger service unavailable or failed for transferId {}: {}", correlationId, idempotencyKey, t.getMessage());
        TransferResultDTO result = new TransferResultDTO();
        result.setTransferId(idempotencyKey);
        result.setStatus("failure");
        return result;
    }

    public Transfer getTransferById(String transferId) {
        Optional<Transfer> optionalTransfer = transferRepository.findByTransferId(transferId);

        if (optionalTransfer.isPresent()) {
            return optionalTransfer.get();
        } else {
            log.error("Transfer with ID {} not found", transferId);
            throw new IllegalArgumentException("Transfer not found with ID: " + transferId);
        }
    }
}
