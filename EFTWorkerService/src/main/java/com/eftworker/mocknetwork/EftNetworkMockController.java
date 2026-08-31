package com.eftworker.mocknetwork;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * Trigger the EFT network acknowledgement simulation for a given batch.
     *
     * Example:
     * POST /api/mock/eftnetwork/ack/3fa85f64-5717-4562-b3fc-2c963f66afa6
     */
    @PostMapping("/ack/{batchId}")
    public ResponseEntity<String> simulateAck(@PathVariable("batchId") UUID batchId) {
        log.info("Received request to simulate EFT ack for batchId={}", batchId);
        simulator.simulateAckForBatch(batchId);
        return ResponseEntity.accepted().body("EFT ack simulation triggered for batchId=" + batchId);
    }
}
