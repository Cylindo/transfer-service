package com.wallet.transfer.controller;

import com.wallet.transfer.dto.TransferRequestDTO;
import com.wallet.transfer.dto.TransferResultDTO;
import com.wallet.transfer.service.TransferService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/transfers")
@Tag(name = "transfers", description = "Operations related to transfers")
public class TransferController {

    private final TransferService transferService;

    public TransferController(TransferService transferService) {
        this.transferService = transferService;
    }

    /**
     * Endpoint to transfer from one account to another.
     *
     * @param transferRequestDTO The data to be used to create a Call.
     * @return The created Call ID.
     */
    @Operation(
            summary = "Create transfer request",
            description = "Creates a new Transfer Request  and returns the TransferResultDTO."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping
    public ResponseEntity<?> createTransfer(@RequestBody TransferRequestDTO transferRequestDTO,
                                            @RequestHeader(value = "Idempotency-Key") String idempotencyKey) {
        try {
            // Delegate to service, which handles idempotency, transferId, and status
            TransferResultDTO transferResultDTO = transferService.transfer(transferRequestDTO, idempotencyKey);
            return ResponseEntity.status(HttpStatus.CREATED).body(transferResultDTO);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Endpoint to fetch transfer status by ID.
     *
     * @param id The ID of the transfer.
     * @return String Transfer status if found, otherwise 404.
     */

    @Operation(
            summary = "Fetch transfer status by ID",
            description = "Fetches the status of a transfer by its ID."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "200",
                    description = "Transfer found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = String.class)
                    )
            ),            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @GetMapping("/{id}")
    public ResponseEntity<String> fetchTransferStatusById(@PathVariable String id) {
        try {
            if (id == null || id.isEmpty()) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid transfer id.");
            }
            String status = transferService.getTransferById(id);
            if (status == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Transfer not found with ID: " + id);
            }
            return ResponseEntity.ok(status);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }

    /**
     * Endpoint to process a batch of transfer requests.
     *
     * @param transferRequests The list of transfer requests.
     * @param idempotencyKeys  The list of idempotency keys corresponding to each request.
     * @return The list of TransferResultDTOs for each processed request.
     */
    @Operation(
            summary = "Process batch transfer requests",
            description = "Processes a batch of transfer requests and returns their results."
    )
    @ApiResponses(value = {
            @ApiResponse(
                    responseCode = "201",
                    description = "Batch processed successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = List.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid input data",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "413",
                    description = "Payload too large",
                    content = @Content(mediaType = "application/json")
            ),
            @ApiResponse(
                    responseCode = "500",
                    description = "Internal server error",
                    content = @Content(mediaType = "application/json")
            )
    })
    @PostMapping(value = "/batch")
    public ResponseEntity<List<TransferResultDTO>> transferBatch(
            @RequestBody List<TransferRequestDTO> transferRequests,
            @RequestHeader(value = "Idempotency-Key") List<String> idempotencyKeys) {

        try {
            if (transferRequests == null || transferRequests.isEmpty()) {
                return ResponseEntity.badRequest().build();
            }
            if (transferRequests.size() > 20) {
                return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build();
            }
            if (idempotencyKeys == null || idempotencyKeys.size() != transferRequests.size()) {
                return ResponseEntity.badRequest().build();
            }
            List<TransferResultDTO> results = transferService.transferBatch(transferRequests, idempotencyKeys);
            return ResponseEntity.status(HttpStatus.CREATED).body(results);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
