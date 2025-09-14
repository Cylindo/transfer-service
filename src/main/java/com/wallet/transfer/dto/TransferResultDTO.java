package com.wallet.transfer.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serial;
import java.io.Serializable;


/**
 * Data Transfer Object for a Transfer Results.
 *
 */
@Data
@Schema(description = "Data Transfer Object representing Transfer results.")
public class TransferResultDTO implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "The unique identifier of the TransferResults", example = "1127723000001565001")
    @JsonProperty("transferId")
    private String transferId;

    @Schema(description = "The status of the transfer", example = "SUCCESS")
    @JsonProperty("status")
    private String status;
}

