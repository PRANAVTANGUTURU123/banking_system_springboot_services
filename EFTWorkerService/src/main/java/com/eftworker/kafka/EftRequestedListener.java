package com.eftworker.kafka;

import com.eftworker.service.EftWorkerService;
import com.events.eft.EftRequested;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class EftRequestedListener {

    private static final Logger log = LoggerFactory.getLogger(EftRequestedListener.class);

    private final EftWorkerService service;
    private final ObjectMapper objectMapper;

    public EftRequestedListener(EftWorkerService service, ObjectMapper objectMapper) {
        this.service = service;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(
            topics = "${eft.topics.requested:eft.requested}",
            groupId = "${spring.kafka.consumer.group-id:eft-worker-service}"
    )
    public void onMessage(ConsumerRecord<String, String> record) {
        log.info("Consumed eft.requested key={} value={}", record.key(), record.value());
        try {
            EftRequested evt = objectMapper.readValue(record.value(), EftRequested.class);
            service.handleRequested(evt);
        } catch (Exception e) {
            log.error("Failed to handle EftRequested message", e);
            throw new RuntimeException(e);
        }
    }
}
