package com.account.config;

import com.account.dto.HoldStatus;
import com.account.repository.AccountHoldRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.LocalDateTime;

@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
public class SchedulingConfig {

    private final AccountHoldRepository holdRepo;

    // Runs every minute: mark ACTIVE holds as EXPIRED if past releaseAt
    @Scheduled(fixedDelay = 60_000)
    public void autoExpireHolds() {
        var now = LocalDateTime.now();
        var toExpire = holdRepo.findByStatusAndReleaseAtLessThanEqual(HoldStatus.ACTIVE, now);

        if (!toExpire.isEmpty()) {
            log.info("Auto-expiring {} holds (<= {})", toExpire.size(), now);
        }

        toExpire.forEach(h -> {
            try {
                h.setStatus(HoldStatus.EXPIRED);
                holdRepo.save(h);
            } catch (ObjectOptimisticLockingFailureException e) {
                // Captured or released concurrently — that outcome wins; skip.
                log.info("Hold {} changed concurrently, not expiring", h.getId());
            }
        });
    }
}
