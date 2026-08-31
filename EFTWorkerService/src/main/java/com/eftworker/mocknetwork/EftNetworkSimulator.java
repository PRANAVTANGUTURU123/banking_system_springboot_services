package com.eftworker.mocknetwork;

import com.eftworker.domain.EftBatchLine;
import com.eftworker.repo.EftBatchLineRepository;
import com.events.eft.EftAckFileMessage;
import com.events.eft.EftAckMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Simulates the EFT network's acknowledgement file for a batch
 * (mirrors Central1Simulator in BillPayWorkerService).
 */
@Component
public class EftNetworkSimulator {

    private static final Logger log = LoggerFactory.getLogger(EftNetworkSimulator.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final EftBatchLineRepository batchLineRepo;

    public EftNetworkSimulator(KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper,
                               EftBatchLineRepository batchLineRepo) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.batchLineRepo = batchLineRepo;
    }

    public void simulateAckForBatch(UUID batchId) {
        List<UUID> paymentIds = batchLineRepo.findAllByBatchId(batchId).stream()
                .map(EftBatchLine::getPaymentId)
                .toList();

        List<EftAckMessage> items = paymentIds.stream()
                .map(pid -> new EftAckMessage(
                        pid,
                        batchId,
                        "E2E-" + pid,
                        "ACSP",
                        "AcceptedSettlementInProcess",
                        true,
                        "Settled successfully",
                        OffsetDateTime.now()
                ))
                .toList();

        EftAckFileMessage file = new EftAckFileMessage(batchId, items, OffsetDateTime.now());

        try {
            kafkaTemplate.send("eftnetwork.ack", batchId.toString(), objectMapper.writeValueAsString(file));
            log.info("Simulated EFT network ack for batch {} with {} items", batchId, items.size());
        } catch (Exception e) {
            log.error("Failed to simulate EFT ack for batch {}", batchId, e);
        }
    }
}
