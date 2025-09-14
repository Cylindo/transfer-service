package com.wallet.transfer.service;

import com.example.common.exception.ValidationError;
import com.example.common.exception.ValidationException;
import com.wallet.transfer.dto.TransferRequestDTO;
import com.wallet.transfer.dto.TransferResultDTO;
import com.wallet.transfer.util.TransferValidator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TransferService {
    TransferServiceProcessor  transferServiceProcessor;

    public TransferService(TransferServiceProcessor transferServiceProcessor) {
        this.transferServiceProcessor = transferServiceProcessor;
    }

    /**
     * Process a transfer between two accounts.
     *
     * @param requestDTO The Transfer data to create.
     * @param idempotencyKey  The idempotencyKey for the request.
     * @return The TransferResultDTO of the created TransferRequestDTO.
     */
    public TransferResultDTO transfer(TransferRequestDTO requestDTO, String idempotencyKey) {
        log.info("Processing transfer with idempotencyKey {}", idempotencyKey);

        TransferResultDTO resultDTO;

        //validate requestDTO
        List<ValidationError> validationErrors = TransferValidator.validateTransformerRequest(requestDTO);
        if (!validationErrors.isEmpty()) {
            log.error("Validation errors: {}", validationErrors);
            throw new IllegalArgumentException("Invalid transfer request: " + validationErrors);
        }
        //delegate to processor
        resultDTO = transferServiceProcessor.processTransfer(requestDTO, idempotencyKey);
        return resultDTO;
    }

    public String getTransferById(String transferId) {
        log.info("Fetching transfer with transferId: {}", transferId);

        //validate input
        List<ValidationError> errors = new ArrayList<>();

        if (transferId == null || transferId.isEmpty()) {
            errors.add(new ValidationError("transferId", "transferId cannot be null or empty.", null));
        }
        // Throw validation Exception if there are any validation errors
        if (!errors.isEmpty()) {
            throw new ValidationException("Validation failed.", errors);
        }

        try {
            return transferServiceProcessor.getTransferById(transferId).getStatus();
        } catch (Exception e) {
            log.error("Error fetching transfer with transferId {}: {}", transferId, e.getMessage());
            throw e;
        }
    }

    public List<TransferResultDTO> transferBatch(List<TransferRequestDTO> transferRequests, List<String> idempotencyKeys) {
        if (transferRequests == null) {
            throw new IllegalArgumentException("Transfer requests must be non-null");
        }
        if (idempotencyKeys == null) {
            throw new IllegalArgumentException("Idempotency keys must be non-null");
        }
        if (transferRequests.size() > 20) {
            throw new IllegalArgumentException("Batch size must not exceed 20");
        }
        if (transferRequests.size() != idempotencyKeys.size()) {
            throw new IllegalArgumentException("The number of idempotency keys must match the number of transfer requests");
        }
        List<CompletableFuture<TransferResultDTO>> futures = new ArrayList<>();
        for (int i = 0; i < transferRequests.size(); i++) {
            final TransferRequestDTO req = transferRequests.get(i);
            final String idempotencyKey = idempotencyKeys.get(i);
            int finalI = i;
            CompletableFuture<TransferResultDTO> future = CompletableFuture.supplyAsync(() -> {
                try {
                    // If you want to pass idempotencyKey to processor, add an overloaded method
                    return transferServiceProcessor.processTransfer(req, idempotencyKey);
                } catch (Exception e) {
                    log.error("Error processing transfer batch item {}: {}", finalI, e.getMessage());
                    TransferResultDTO failed = new TransferResultDTO();
                    failed.setStatus("failure");
                    failed.setTransferId(null);
                    return failed;
                }
            });
            futures.add(future);
        }
        return futures.stream()
                .map(f -> {
                    try {
                        return f.get();
                    } catch (InterruptedException | ExecutionException e) {
                        log.error("Error waiting for transfer batch result: {}", e.getMessage());
                        TransferResultDTO failed = new TransferResultDTO();
                        failed.setStatus("failure");
                        failed.setTransferId(null);
                        return failed;
                    }
                })
                .collect(Collectors.toList());
    }
}
