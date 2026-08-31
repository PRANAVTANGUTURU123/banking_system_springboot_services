package com.payments.orch.controller;

import com.payments.orch.dto.BillPayRequest;
import com.payments.orch.dto.EftPayRequest;
import com.payments.orch.dto.PaymentAcceptedResponse;
import com.payments.orch.service.BillPayOrchestrator;
import com.payments.orch.service.EftOrchestrator;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PaymentController {

  private final BillPayOrchestrator billPayOrchestrator;
  private final EftOrchestrator eftOrchestrator;

  @PostMapping("/payments/eft")
  public ResponseEntity<PaymentAcceptedResponse> eft(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody EftPayRequest req
  ) {
    var res = eftOrchestrator.acceptEft(req, idempotencyKey);
    var location = URI.create("/api/v1/payments/" + res.paymentId());
    return ResponseEntity.accepted().location(location).body(res);
  }

  @PostMapping("/payments/billpay")
  public ResponseEntity<PaymentAcceptedResponse> billPay(
      @RequestHeader("Idempotency-Key") String idempotencyKey,
      @Valid @RequestBody BillPayRequest req
  ) {
    var res = billPayOrchestrator.acceptBillPay(req, idempotencyKey);
    var location = URI.create("/api/v1/payments/" + res.paymentId());
    return ResponseEntity.accepted().location(location).body(res);
  }

  @GetMapping("/payments/{paymentId}")
  public ResponseEntity<?> get(@PathVariable UUID paymentId) {
    return ResponseEntity.ok(billPayOrchestrator.view(paymentId));
  }
}
