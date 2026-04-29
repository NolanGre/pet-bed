package op.edu.ua.petbed.lost.infrastructure.job;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.lost.domain.repository.FoundRequestRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

@Component
@NullMarked
@Slf4j
@RequiredArgsConstructor
public class FoundRequestCleanupJob {

    private final FoundRequestRepository foundRequestRepository;
    private static final Duration RETENTION_PERIOD = Duration.ofDays(365);

    @Scheduled(cron = "0 0 0 * * ?", zone = "UTC") // Runs daily at 00:00 UTC
    @Transactional
    public void cleanupOldFoundRequests() {
        log.info("Starting cleanup of old found requests");

        try {
            Instant cutoff = Instant.now().minus(RETENTION_PERIOD);
            int deletedCount = foundRequestRepository.deleteOlderThan(cutoff);
            log.info("Successfully deleted {} found requests older than {}", deletedCount, cutoff);
        } catch (Exception e) {
            log.error("Failed to cleanup old found requests", e);
            // Don't re-throw to prevent scheduler from stopping
        }
    }
}
