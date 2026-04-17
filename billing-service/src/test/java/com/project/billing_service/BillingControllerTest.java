package com.project.billing_service;

import com.project.billing_service.controller.BillingController;
import com.project.billing_service.model.dtos.InvoiceDto;
import com.project.billing_service.model.entities.InvoiceEntity;
import com.project.billing_service.service.BillingService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.AutoConfigureJsonTesters;
import org.springframework.boot.test.json.JacksonTester;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@WebMvcTest(BillingController.class)
@AutoConfigureJsonTesters
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class BillingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JacksonTester<InvoiceDto> json;

    @MockitoBean
    private BillingService billingService;

    @Test
    void createInvoice_shouldReturnCreated() throws Exception {

        UUID subscriptionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();

        InvoiceDto dto = InvoiceDto.builder()
                .subscriptionId(subscriptionId)
                .customerId(customerId)
                .amount(BigDecimal.valueOf(150.00))
                .currency("USD")
                .status("ISSUED")
                .issuedAt(OffsetDateTime.now())
                .dueAt(OffsetDateTime.now().plusDays(7))
                .build();

        InvoiceEntity entity = InvoiceEntity.builder()
                .invoiceId(UUID.randomUUID())
                .subscriptionId(subscriptionId)
                .customerId(customerId)
                .amount(dto.getAmount())
                .currency(dto.getCurrency())
                .status(dto.getStatus())
                .issuedAt(dto.getIssuedAt())
                .dueAt(dto.getDueAt())
                .build();

        Mockito.when(billingService.createInvoice(Mockito.any()))
                .thenReturn(entity);

        mockMvc.perform(post("/api/billing/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.write(dto).getJson()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceId").value(entity.getInvoiceId().toString()))
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId.toString()))
                .andExpect(jsonPath("$.currency").value("USD"));

        ArgumentCaptor<InvoiceDto> captor = ArgumentCaptor.forClass(InvoiceDto.class);
        Mockito.verify(billingService).createInvoice(captor.capture());
        InvoiceDto capturedDto = captor.getValue();

        assertEquals(subscriptionId, capturedDto.getSubscriptionId());
        assertEquals(customerId, capturedDto.getCustomerId());
        assertEquals("USD", capturedDto.getCurrency());
    }
}
