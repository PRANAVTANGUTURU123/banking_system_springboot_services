package com.billpay.worker.mocksettlement;


import com.billpay.dto.Pain002FileMessage;
import com.billpay.worker.domain.BatchLine;
import com.billpay.worker.repo.BatchLineRepository;

import com.events.Topics;
import com.events.billpay.Pain002Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;


/**
 * Stands in for Central 1: publishes a pain.002 status report for a batch.
 * This is the external network, so it sends straight to Kafka (no outbox).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class Central1Simulator {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final BatchLineRepository batchLineRepo;

    /** @return number of payments in the simulated file */
    public int simulatePain002ForBatch(UUID batchId, Set<UUID> reject, boolean rejectAll) {
        List<Pain002Message> items = batchLineRepo.findAllByBatchId(batchId).stream()
                .map(BatchLine::getPaymentId)
                .map(pid -> (rejectAll || reject.contains(pid))
                        // AC04 = ISO 20022 "closed account number"
                        ? new Pain002Message(pid, batchId, "E2E-" + pid, "RJCT", "Rejected",
                                false, "AC04: creditor account closed", OffsetDateTime.now())
                        : new Pain002Message(pid, batchId, "E2E-" + pid, "ACTC", "Accepted",
                                true, "Posted successfully", OffsetDateTime.now()))
                .toList();

        Pain002FileMessage file = new Pain002FileMessage(batchId, items, OffsetDateTime.now());

        try {
            kafkaTemplate.send(Topics.CENTRAL1_PAIN002, batchId.toString(), objectMapper.writeValueAsString(file)).get();
            log.info("Simulated pain.002 file for batch {} with {} items", batchId, items.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish simulated pain.002 for batch " + batchId, e);
        }
        return items.size();
    }
}
