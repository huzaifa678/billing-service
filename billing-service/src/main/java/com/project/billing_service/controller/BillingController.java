package com.project.billing_service.controller;

import com.project.billing_service.model.dtos.InvoiceDto;
import com.project.billing_service.model.dtos.PaymentDto;
import com.project.billing_service.model.entities.InvoiceEntity;
import com.project.billing_service.service.BillingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import subscription.Subscription;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/billing")
@RequiredArgsConstructor
public class BillingController {

    private final BillingService billingService;

    @PostMapping("/invoices")
    public ResponseEntity<InvoiceEntity> createInvoice(@RequestBody InvoiceDto dto) {
        InvoiceEntity invoice = billingService.createInvoice(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<InvoiceEntity> getInvoice(@PathVariable UUID id) {
        InvoiceEntity invoice = billingService.getInvoice(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(invoice);
    }

    @PostMapping("/invoices/{id}/pay")
    public ResponseEntity<String> payInvoice(
            @PathVariable UUID id,
            @RequestBody PaymentDto dto
    ) {
        String status = billingService.payInvoice(id, dto.getMethodId());
        return ResponseEntity.ok(status);
    }

    @GetMapping("/active")
    public ResponseEntity<List<Subscription.SubscriptionResponse>> getActiveSubscriptions(
            @RequestParam UUID userId
    ) {
        List<Subscription.SubscriptionResponse> subscriptions =
                billingService.getActiveSubscriptions(userId);
        return ResponseEntity.ok(subscriptions);
    }
    
}
