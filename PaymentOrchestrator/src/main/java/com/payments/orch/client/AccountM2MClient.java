package com.payments.orch.client;

import java.util.UUID;

import com.account.dto.HoldResponse;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/** Service-to-service (client-credentials) calls used when a payment reaches a final state. */
@FeignClient(
        name = "account-servicem2m",
        url = "${account.service.url}",
        configuration = com.payments.orch.security.FeignM2MOAuth2Config.class
)
public interface AccountM2MClient {

	  /** Payment failed: give the held funds back. Idempotent on the AccountService side. */
	  @PostMapping("/api/v1/accounts/{accountId}/holds/{holdId}/release")
	  HoldResponse releaseHold(
	      @PathVariable("accountId") UUID accountId,
	      @PathVariable("holdId") UUID holdId
	  );

	  /** Payment posted: hold -> debit in one atomic, idempotent step. */
	  @PostMapping("/api/v1/accounts/{accountId}/holds/{holdId}/capture")
	  HoldResponse captureHold(
	      @PathVariable("accountId") UUID accountId,
	      @PathVariable("holdId") UUID holdId,
	      @RequestParam("reason") String reason
	  );
}
