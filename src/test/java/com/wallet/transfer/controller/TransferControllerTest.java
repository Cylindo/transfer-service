package com.wallet.transfer.controller;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.transfer.dto.TransferRequestDTO;
import com.wallet.transfer.dto.TransferResultDTO;
import com.wallet.transfer.service.TransferService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TransferController.class)
class TransferControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private TransferService transferService;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void transfer_success() throws Exception {
        TransferRequestDTO request = new TransferRequestDTO();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.TEN);

        TransferResultDTO result = new TransferResultDTO();
        result.setStatus("success");

        Mockito.when(transferService.transfer(any(TransferRequestDTO.class), any()))
                .thenReturn(result);

        mockMvc.perform(post("/transfers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Idempotency-Key", "test-key")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void fetchTransferStatusById_notFound() throws Exception {
        String transferId = "nonexistent-id";
        Mockito.when(transferService.getTransferById(transferId))
                .thenThrow(new IllegalArgumentException("Transfer not found with ID: " + transferId));

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/transfers/{id}", transferId)
                )
                .andExpect(status().isBadRequest()) // or .isNotFound() if your handler returns 404
                .andExpect(jsonPath("$.message").value("Transfer not found with ID: " + transferId));
    }

    @Test
    void fetchTransferStatusById_success() throws Exception {
        String transferId = "existing-id";
        String expectedStatus = "success";

        Mockito.when(transferService.getTransferById(transferId))
                .thenReturn(expectedStatus);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/transfers/{id}", transferId)
                )
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(expectedStatus));
    }
}
