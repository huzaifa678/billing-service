package com.project.billing;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.billing.adapter.in.rest.BillingController;
import com.project.billing.adapter.in.rest.InvoiceRestMapper;
import com.project.billing.adapter.in.rest.dto.InvoiceRequest;
import com.project.billing.application.invoice.port.in.CreateInvoiceCommand;
import com.project.billing.application.invoice.port.in.CreateInvoiceUseCase;
import com.project.billing.application.invoice.port.in.InvoiceQueries;
import com.project.billing.application.invoice.port.in.PayInvoiceUseCase;
import com.project.billing.application.invoice.port.in.SubscriptionQueries;
import com.project.billing.domain.invoice.Invoice;
import com.project.billing.domain.invoice.InvoiceId;
import com.project.billing.domain.invoice.InvoiceStatus;
import com.project.billing.domain.shared.CustomerId;
import com.project.billing.domain.shared.Money;
import com.project.billing.domain.shared.SubscriptionId;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = BillingController.class)
@Import(InvoiceRestMapper.class)
@ActiveProfiles("test")
class BillingControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateInvoiceUseCase createInvoiceUseCase;
    @MockitoBean
    private PayInvoiceUseCase payInvoiceUseCase;
    @MockitoBean
    private InvoiceQueries invoiceQueries;
    @MockitoBean
    private SubscriptionQueries subscriptionQueries;

    @Test
    void createInvoice_shouldReturnCreated() throws Exception {
        UUID subscriptionId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        UUID invoiceId = UUID.randomUUID();

        InvoiceRequest request = new InvoiceRequest(
                null, subscriptionId, customerId, new BigDecimal("150.00"), "USD",
                "ISSUED", OffsetDateTime.now(), OffsetDateTime.now().plusDays(7)
        );

        Invoice created = Invoice.reconstitute(
                InvoiceId.of(invoiceId),
                SubscriptionId.of(subscriptionId),
                CustomerId.of(customerId),
                Money.of("150.00", "USD"),
                InvoiceStatus.ISSUED,
                request.issuedAt(),
                request.dueAt()
        );
        when(createInvoiceUseCase.create(any())).thenReturn(created);

        mockMvc.perform(post("/api/billing/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceId").value(invoiceId.toString()))
                .andExpect(jsonPath("$.subscriptionId").value(subscriptionId.toString()))
                .andExpect(jsonPath("$.currency").value("USD"));

        ArgumentCaptor<CreateInvoiceCommand> captor = ArgumentCaptor.forClass(CreateInvoiceCommand.class);
        verify(createInvoiceUseCase).create(captor.capture());
        assertEquals(subscriptionId, captor.getValue().subscriptionId());
        assertEquals(customerId, captor.getValue().customerId());
        assertEquals("USD", captor.getValue().currency());
    }
}
