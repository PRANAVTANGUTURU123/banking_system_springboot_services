package com.eft.controller;


import com.eft.dto.ExternalAccountRequest;
import com.eft.dto.ExternalAccountResponse;
import com.eft.service.ExternalAccountService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/external-accounts")
public class ExternalAccountController {

    private final ExternalAccountService service;

    public ExternalAccountController(ExternalAccountService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ExternalAccountResponse> register(@Valid @RequestBody ExternalAccountRequest req) {
        ExternalAccountResponse res = service.register(req);
        return ResponseEntity.created(URI.create("/api/v1/external-accounts/" + res.id())).body(res);
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<ExternalAccountResponse> verify(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.verify(id));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExternalAccountResponse> get(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping
    public ResponseEntity<List<ExternalAccountResponse>> listByCustomer(
            @RequestParam("customerId") String customerId) {
        return ResponseEntity.ok(service.listByCustomer(customerId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> disable(@PathVariable("id") UUID id) {
        service.disable(id);
        return ResponseEntity.noContent().build();
    }

    /** Registry-style lookup used by PaymentOrchestrator (mirrors /billers/{ref}/active). */
    @GetMapping("/{id}/active")
    public ResponseEntity<Boolean> isActive(@PathVariable("id") UUID id) {
        return ResponseEntity.ok(service.isActive(id));
    }
}
