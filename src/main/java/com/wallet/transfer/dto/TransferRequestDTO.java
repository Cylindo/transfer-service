package com.wallet.transfer.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Request DTO for transferring funds between accounts.")
public class TransferRequestDTO {

    @Schema(description = "ID of the account to debit", example = "1001")
    @NotNull
    private Long fromAccountId;

    @Schema(description = "ID of the account to credit", example = "1002")
    @NotNull
    private Long toAccountId;

    @Schema(description = "Amount to transfer", example = "250.00")
    @NotNull
    @Positive
    private BigDecimal amount;
}