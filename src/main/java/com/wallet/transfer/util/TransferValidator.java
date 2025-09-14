package com.wallet.transfer.util;

import com.example.common.exception.ValidationError;
import com.wallet.transfer.dto.TransferRequestDTO;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransferValidator {
    public static List<ValidationError> validateTransformerRequest(TransferRequestDTO transferRequestDTO) {
        List<ValidationError> errors = new ArrayList<>();

        if (transferRequestDTO == null) {
            errors.add(new ValidationError("transferRequestDTO", "TransferRequestDTO must not be null",null));
            return errors;
        }
        if (transferRequestDTO.getFromAccountId() == null || transferRequestDTO.getFromAccountId() <= 0) {
            errors.add(new ValidationError("fromAccountId", "fromAccountId must not be null",null));
        }
        if (transferRequestDTO.getToAccountId() == null || transferRequestDTO.getToAccountId() <= 0) {
            errors.add(new ValidationError("toAccountId", "toAccountId must not be null",null));

        }
        if (Objects.equals(transferRequestDTO.getFromAccountId(), transferRequestDTO.getToAccountId())) {
            errors.add(new ValidationError("fromAccountId","FromAccountId and ToAccountId must be different",null));
        }
        if (transferRequestDTO.getAmount() == null) {
            errors.add(new ValidationError("toAccountId","amount must not be null",null));

        }
        if (transferRequestDTO.getAmount() != null && transferRequestDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new ValidationError("amount","Amount must be greater than zero",null));
        }
        return errors;
    }
}
