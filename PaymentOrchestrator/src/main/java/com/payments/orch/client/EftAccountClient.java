package com.payments.orch.client;

import java.util.UUID;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "eft-registry", url = "${eft.service.url}")
public interface EftAccountClient {
  @GetMapping("/api/v1/external-accounts/{id}/active")
  boolean isActive(@PathVariable("id") UUID externalAccountId);
}
