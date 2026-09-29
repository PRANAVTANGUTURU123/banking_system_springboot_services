package com.eftworker.mocknetwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/mock/eftnetwork")
public class EftNetworkMockController {

    private static final Logger log = LoggerFactory.getLogger(EftNetworkMockController.class);

    private final EftNetworkSimulator simulator;

    public EftNetworkMockController(EftNetworkSimulator simulator) {
        this.simulator = simulator;
    }

    /**
     * Trigger the EFT network acknowledgement simulation for a given batch. Every
     * payment is settled unless rejected via {@code reject} (repeatable) or {@code rejectAll}.
     *
     * Examples:
     * POST /api/mock/eftnetwork/ack/{batchId}
     * POST /api/mock/eftnetwork/ack/{batchId}?reject={paymentId}
     * POST /api/mock/eftnetwork/ack/{batchId}?rejectAll=true
     */
    @PostMapping("/ack/{batchId}")
    public ResponseEntity<String> simulateAck(
            @PathVariable("batchId") UUID batchId,
            @RequestParam(name = "reject", required = false) Set<UUID> reject,
            @RequestParam(name = "rejectAll", defaultValue = "false") boolean rejectAll) {
        log.info("Received request to simulate EFT ack for batchId={}", batchId);
        int items = simulator.simulateAckForBatch(batchId, reject == null ? Set.of() : reject, rejectAll);
        return ResponseEntity.accepted()
                .body("EFT ack simulation triggered for batchId=" + batchId + " (" + items + " payments)");
    }
}
