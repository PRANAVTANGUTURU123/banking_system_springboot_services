package com.eftworker.mocknetwork;

import com.eftworker.domain.EftBatchLine;
import com.eftworker.repo.EftBatchLineRepository;
import com.events.Topics;
import com.events.eft.EftAckFileMessage;
import com.events.eft.EftAckMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Simulates the EFT network's acknowledgement file for a batch
 * (mirrors Central1Simulator in BillPayWorkerService). This is the external
 * network, so it sends straight to Kafka (no outbox).
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

    /** @return number of payments in the simulated ack file */
    public int simulateAckForBatch(UUID batchId, Set<UUID> reject, boolean rejectAll) {
        List<EftAckMessage> items = batchLineRepo.findAllByBatchId(batchId).stream()
                .map(EftBatchLine::getPaymentId)
                .map(pid -> (rejectAll || reject.contains(pid))
                        // 912 = CPA-005 return reason "account closed"
                        ? new EftAckMessage(pid, batchId, "E2E-" + pid, "RJCT", "Rejected",
                                false, "912: payee account closed", OffsetDateTime.now())
                        : new EftAckMessage(pid, batchId, "E2E-" + pid, "ACSP", "AcceptedSettlementInProcess",
                                true, "Settled successfully", OffsetDateTime.now()))
                .toList();

        EftAckFileMessage file = new EftAckFileMessage(batchId, items, OffsetDateTime.now());

        try {
            kafkaTemplate.send(Topics.EFTNETWORK_ACK, batchId.toString(), objectMapper.writeValueAsString(file)).get();
            log.info("Simulated EFT network ack for batch {} with {} items", batchId, items.size());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish simulated EFT ack for batch " + batchId, e);
        }
        return items.size();
    }
}
