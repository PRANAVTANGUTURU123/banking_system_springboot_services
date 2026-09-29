package com.settlement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Stub client that represents uploading the pain.001 file to Central1.
 * Replace with REST/SFTP implementation as needed.
 *
 * <p>{@code mock.central1.upload-failure-rate} (0.0–1.0) makes uploads fail at
 * random so the retry and dead-letter paths can be exercised; 1.0 always fails.</p>
 */
@Component
@Slf4j
public class Central1Client {

    private final double failureRate;

    public Central1Client(@Value("${mock.central1.upload-failure-rate:0}") double failureRate) {
        this.failureRate = failureRate;
    }

    public String upload(String fileName) {
        log.info("Uploading pain.001 file={} to Central1...", fileName);
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new IllegalStateException("Central1 upload failed (simulated): connection reset");
        }
        return "CENTRAL1-" + System.currentTimeMillis();
    }
}
