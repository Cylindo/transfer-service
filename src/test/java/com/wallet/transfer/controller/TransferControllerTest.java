package com.wallet.transfer.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wallet.transfer.dto.TransferRequestDTO;
import com.wallet.transfer.dto.TransferResultDTO;
import com.wallet.transfer.service.TransferService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransferService transferService;

    private ObjectMapper objectMapper;

    @InjectMocks
    private TransferController transferController;

    @BeforeEach
    void setup() {
        mockMvc = MockMvcBuilders.standaloneSetup(transferController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void transfer_success() throws Exception {
        TransferRequestDTO request = new TransferRequestDTO();
        request.setFromAccountId(1L);
        request.setToAccountId(2L);
        request.setAmount(BigDecimal.TEN);

        TransferResultDTO result = new TransferResultDTO();
        result.setStatus("SUCCESS"); // Match the actual service return value

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
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string("Transfer not found with ID: " + transferId));
    }

    @Test
    void fetchTransferStatusById_success() throws Exception {
        String transferId = "existing-id";
        String expectedStatus = "SUCCESS";

        Mockito.when(transferService.getTransferById(transferId))
                .thenReturn(expectedStatus);

        mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/transfers/{id}", transferId)
                )
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.content().string(expectedStatus));
    }
}
