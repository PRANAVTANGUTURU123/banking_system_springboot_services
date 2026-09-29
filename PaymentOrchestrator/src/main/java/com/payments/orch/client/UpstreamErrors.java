package com.payments.orch.client;

import com.commons.exception.BadRequestException;
import com.commons.exception.ConflictException;
import com.commons.exception.InsufficientFundsException;
import com.commons.exception.ResourceNotFoundException;
import com.commons.exception.UpstreamException;
import feign.FeignException;
import org.springframework.security.access.AccessDeniedException;

import java.util.function.Supplier;

/**
 * Runs a Feign call and turns downstream HTTP errors into our domain exceptions,
 * so the client gets e.g. 422 INSUFFICIENT_FUNDS instead of a generic 500.
 */
public final class UpstreamErrors {

  private UpstreamErrors() {}

  public static <T> T call(String service, Supplier<T> call) {
    try {
      return call.get();
    } catch (FeignException e) {
      String detail = service + " responded " + e.status() + ": " + e.contentUTF8();
      throw switch (e.status()) {
        case 400 -> new BadRequestException(detail);
        case 401, 403 -> new AccessDeniedException(detail);
        case 404 -> new ResourceNotFoundException(detail);
        case 409 -> new ConflictException(detail);
        case 422 -> new InsufficientFundsException();
        default -> new UpstreamException(detail, e);
      };
    }
  }
}
