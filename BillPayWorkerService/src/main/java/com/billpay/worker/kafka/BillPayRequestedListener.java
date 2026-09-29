package com.billpay.worker.kafka;

import com.events.Topics;
import com.events.billpay.*;
import com.billpay.worker.service.BillPayWorkerService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class BillPayRequestedListener {

    private final BillPayWorkerService service;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = Topics.BILLPAY_REQUESTED,
            groupId = "${spring.kafka.consumer.group-id:billpay-worker-v1}"
    )
    public void onMessage(ConsumerRecord<String, String> record) throws Exception {
        log.info("Consumed billpay.requested key={}", record.key());
        // Exceptions propagate to the container's error handler, which retries the record
        service.handleRequested(objectMapper.readValue(record.value(), BillPayRequested.class));
    }
}
