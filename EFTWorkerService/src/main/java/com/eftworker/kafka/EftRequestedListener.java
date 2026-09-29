package com.eftworker.kafka;

import com.eftworker.service.EftWorkerService;
import com.events.Topics;
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
            topics = Topics.EFT_REQUESTED,
            groupId = "${spring.kafka.consumer.group-id:eft-worker-service}"
    )
    public void onMessage(ConsumerRecord<String, String> record) throws Exception {
        log.info("Consumed eft.requested key={}", record.key());
        // Exceptions propagate to the container's error handler, which retries the record
        service.handleRequested(objectMapper.readValue(record.value(), EftRequested.class));
    }
}
