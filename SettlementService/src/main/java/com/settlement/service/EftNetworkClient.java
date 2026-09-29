package com.settlement.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Stub client that represents uploading the CPA-005 file to the EFT network.
 *
 * <p>{@code mock.eftnetwork.upload-failure-rate} (0.0–1.0) makes uploads fail at
 * random so the retry and dead-letter paths can be exercised; 1.0 always fails.</p>
 */
@Component
@Slf4j
public class EftNetworkClient {

    private final double failureRate;

    public EftNetworkClient(@Value("${mock.eftnetwork.upload-failure-rate:0}") double failureRate) {
        this.failureRate = failureRate;
    }

    public String upload(String fileName) {
        log.info("Uploading CPA-005 file={} to EFT network...", fileName);
        if (ThreadLocalRandom.current().nextDouble() < failureRate) {
            throw new IllegalStateException("EFT network upload failed (simulated): connection reset");
        }
        return "EFTNET-" + System.currentTimeMillis();
    }
}
