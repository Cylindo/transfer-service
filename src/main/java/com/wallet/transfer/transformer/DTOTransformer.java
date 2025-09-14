package com.wallet.transfer.transformer;

import com.wallet.transfer.dto.TransferResultDTO;
import com.wallet.transfer.entity.Transfer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DTOTransformer {
    public static TransferResultDTO transferResultDTO(Transfer transfer) {

        TransferResultDTO dto = new TransferResultDTO();
        dto.setTransferId(String.valueOf(transfer.getId()));
        dto.setStatus(transfer.getStatus());
        log.info("Transformed Transfer entity with ID {} to TransferResultDTO.", transfer.getId());
        return dto;
    }
}
