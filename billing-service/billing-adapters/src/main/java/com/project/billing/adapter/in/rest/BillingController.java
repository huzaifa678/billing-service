package com.project.billing.adapter.in.rest;

import com.project.billing.adapter.in.rest.dto.InvoiceRequest;
import com.project.billing.adapter.in.rest.dto.InvoiceResponse;
import com.project.billing.adapter.in.rest.dto.PaymentRequest;
import com.project.billing.adapter.in.rest.dto.SubscriptionResponse;
import com.project.billing.application.invoice.port.in.CreateInvoiceUseCase;
import com.project.billing.application.invoice.port.in.InvoiceQueries;
import com.project.billing.application.invoice.port.in.PayInvoiceCommand;
import com.project.billing.application.invoice.port.in.PayInvoiceUseCase;
import com.project.billing.application.invoice.port.in.SubscriptionQueries;
import com.project.billing.domain.invoice.Invoice;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final CreateInvoiceUseCase createInvoiceUseCase;
    private final PayInvoiceUseCase payInvoiceUseCase;
    private final InvoiceQueries invoiceQueries;
    private final SubscriptionQueries subscriptionQueries;
    private final InvoiceRestMapper mapper;

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceResponse> createInvoice(@RequestBody InvoiceRequest request) {
        Invoice invoice = createInvoiceUseCase.create(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(mapper.toResponse(invoice));
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<InvoiceResponse> getInvoice(@PathVariable UUID id) {
        Invoice invoice = invoiceQueries.getById(id);
        return ResponseEntity.ok(mapper.toResponse(invoice));
    }

    @PostMapping("/invoices/{id}/pay")
    public ResponseEntity<String> payInvoice(
            @PathVariable UUID id,
            @RequestBody PaymentRequest request
    ) {
        String status = payInvoiceUseCase.pay(new PayInvoiceCommand(id, request.methodId()));
        return ResponseEntity.ok(status);
    }

    @GetMapping("/active")
    public ResponseEntity<List<SubscriptionResponse>> getActiveSubscriptions(
            @RequestParam UUID userId
    ) {
        List<SubscriptionResponse> subscriptions = subscriptionQueries.activeByUser(userId)
                .stream()
                .map(mapper::toResponse)
                .toList();
        return ResponseEntity.ok(subscriptions);
    }
}
