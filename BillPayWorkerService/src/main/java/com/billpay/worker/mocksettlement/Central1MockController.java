package com.billpay.worker.mocksettlement;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/mock/central1")
@RequiredArgsConstructor
@Slf4j
public class Central1MockController {

    private final Central1Simulator central1Simulator;

    /**
     * Trigger Central 1 pain.002 simulation for a given batch. Every payment is
     * accepted unless rejected via {@code reject} (repeatable) or {@code rejectAll}.
     *
     * Examples:
     * POST /api/mock/central1/pain002/{batchId}
     * POST /api/mock/central1/pain002/{batchId}?reject={paymentId}
     * POST /api/mock/central1/pain002/{batchId}?rejectAll=true
     */
    @PostMapping("/pain002/{batchId}")
    public ResponseEntity<String> simulatePain002(
            @PathVariable("batchId") UUID batchId,
            @RequestParam(name = "reject", required = false) Set<UUID> reject,
            @RequestParam(name = "rejectAll", defaultValue = "false") boolean rejectAll) {
        log.info("Received request to simulate pain.002 for batchId={}", batchId);

        int items = central1Simulator.simulatePain002ForBatch(batchId, reject == null ? Set.of() : reject, rejectAll);

        return ResponseEntity.accepted()
                .body("Pain.002 simulation triggered for batchId=" + batchId + " (" + items + " payments)");
    }
}
