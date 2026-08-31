package com.eftworker.service;

import com.eftworker.domain.EftBatch;
import com.eftworker.domain.EftBatchLine;
import com.eftworker.repo.EftBatchLineRepository;
import com.eftworker.repo.EftBatchRepository;
import com.events.eft.EftBatchReadyEvent;
import com.events.eft.EftEnqueued;
import com.events.eft.EftRequested;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
public class EftWorkerService {

    private static final Logger log = LoggerFactory.getLogger(EftWorkerService.class);

    private final EftBatchRepository batches;
    private final EftBatchLineRepository lines;
    private final KafkaTemplate<String, String> kafka;
    private final ObjectMapper objectMapper;

    @Value("${eft.topics.enqueued:eft.enqueued}")
    private String enqueuedTopic;

    @Value("${eft.topics.batch-ready:eft.batch.ready}")
    private String batchReadyTopic;

    @Value("${eft.batch.threshold:1}")
    private int batchThreshold;

    public EftWorkerService(EftBatchRepository batches,
                            EftBatchLineRepository lines,
                            KafkaTemplate<String, String> kafka,
                            ObjectMapper objectMapper) {
        this.batches = batches;
        this.lines = lines;
        this.kafka = kafka;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void handleRequested(EftRequested evt) {
        log.info("Handling EftRequested paymentId={} externalAccountId={}",
                evt.paymentId(), evt.externalAccountId());

        // 1) find or create OPEN batch
        EftBatch batch = batches.findFirstByStatusOrderByCreatedAtAsc("OPEN")
                .orElseGet(() -> {
                    UUID batchId = UUID.randomUUID();
                    EftBatch b = new EftBatch(batchId, "OPEN", OffsetDateTime.now());
                    batches.save(b);
                    log.info("Opened new EFT batch {}", batchId);
                    return b;
                });

        long currentLines = lines.countByBatchId(batch.getBatchId());
        int nextLineNo = (int) currentLines + 1;

        lines.save(new EftBatchLine(batch.getBatchId(), evt.paymentId(), nextLineNo, OffsetDateTime.now()));
        log.info("Enqueued EFT payment {} into batch {} lineNo={}",
                evt.paymentId(), batch.getBatchId(), nextLineNo);

        // 2) Emit eft.enqueued
        EftEnqueued enq = new EftEnqueued(
                UUID.randomUUID().toString(),
                evt.paymentId(),
                batch.getBatchId(),
                OffsetDateTime.now().toString(),
                "1",
                "eft"
        );
        send(enqueuedTopic, evt.paymentId().toString(), enq);

        // 3) If threshold reached, mark batch ready and emit eft.batch.ready
        if (nextLineNo >= batchThreshold && "OPEN".equals(batch.getStatus())) {
            batch.setStatus("READY_EMITTED");
            batches.save(batch);

            EftBatchReadyEvent ready = new EftBatchReadyEvent(
                    UUID.randomUUID().toString(),
                    batch.getBatchId(),
                    OffsetDateTime.now().toString(),
                    "1",
                    "eft"
            );
            send(batchReadyTopic, batch.getBatchId().toString(), ready);
            log.info("EFT batch {} reached threshold {}, emitted EftBatchReady",
                    batch.getBatchId(), batchThreshold);
        }
    }

    private void send(String topic, String key, Object payload) {
        try {
            kafka.send(topic, key, objectMapper.writeValueAsString(payload));
        } catch (Exception e) {
            log.error("Failed to serialize event for topic {} key {}", topic, key, e);
            throw new RuntimeException(e);
        }
    }
}
