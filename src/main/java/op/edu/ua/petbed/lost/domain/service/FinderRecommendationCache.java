package op.edu.ua.petbed.lost.domain.service;

import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * In-memory cache for storing finder recommendations with TTL-based eviction.
 * Used for "Found → Lost" matching flow where finders view potential owner matches.
 * <p>
 * Key characteristics:
 * <ul>
 *   <li>FIFO behavior - recommendations are consumed in order</li>
 *   <li>TTL of 1 day - expired entries are automatically evicted</li>
 *   <li>Thread-safe - uses ConcurrentHashMap and ConcurrentLinkedQueue</li>
 *   <li>One-time viewing - once declined, recommendations are unavailable forever</li>
 * </ul>
 *
 * @see op.edu.ua.petbed.lost.application.matching.MatchingService
 */
@Component
@NullMarked
@Slf4j
public class FinderRecommendationCache {

    private final ConcurrentHashMap<Long, Queue<Long>> cache = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Instant> timestamps = new ConcurrentHashMap<>();

    private static final Duration TTL = Duration.ofDays(1);

    /**
     * Stores recommendations for a finder.
     * Creates a FIFO queue from the provided lost request IDs.
     * Overwrites any existing recommendations for the finder.
     *
     * @param finderId        the Telegram user ID of the finder
     * @param lostRequestIds  list of lost request IDs ordered by matching score (highest first)
     */
    public void put(Long finderId, List<Long> lostRequestIds) {
        cache.put(finderId, new ConcurrentLinkedQueue<>(lostRequestIds));
        timestamps.put(finderId, Instant.now());

        log.info("Stored {} recommendations for finderId={}", lostRequestIds.size(), finderId);
    }

    /**
     * Retrieves and removes the next recommendation from the queue.
     * Returns empty if no recommendations exist or if TTL has expired.
     *
     * @param finderId the Telegram user ID of the finder
     * @return the next lost request ID, or empty if none available
     */
    public Optional<Long> pollNext(Long finderId) {
        cleanIfExpired(finderId);

        Queue<Long> queue = cache.get(finderId);
        if (queue == null || queue.isEmpty()) {
            return Optional.empty();
        }

        Long nextId = queue.poll();
        log.debug("Polled next recommendation for finderId={}: lostRequestId={}", finderId, nextId);

        return Optional.of(nextId);
    }

    /**
     * Checks if the finder has any available recommendations.
     * Returns false if cache is empty or TTL has expired.
     *
     * @param finderId the Telegram user ID of the finder
     * @return true if there are recommendations available
     */
    public boolean hasRecommendations(Long finderId) {
        cleanIfExpired(finderId);

        Queue<Long> queue = cache.get(finderId);
        return queue != null && !queue.isEmpty();
    }

    /**
     * Removes all recommendations for a finder.
     * Called when finder declines viewing or when cache expires.
     *
     * @param finderId the Telegram user ID of the finder
     */
    public void remove(Long finderId) {
        cache.remove(finderId);
        timestamps.remove(finderId);

        log.info("Removed recommendations for finderId={}", finderId);
    }

    /**
     * Scheduled cleanup of expired entries.
     * Runs every hour to remove entries older than TTL.
     */
    @Scheduled(fixedRate = 3_600_000) // 1 hour in milliseconds
    public void evictExpired() {
        Instant cutoff = Instant.now().minus(TTL);
        int evictedCount = 0;

        for (var entry : timestamps.entrySet()) {
            Long finderId = entry.getKey();
            Instant timestamp = entry.getValue();

            if (timestamp.isBefore(cutoff)) {
                cache.remove(finderId);
                timestamps.remove(finderId);
                evictedCount++;

                log.debug("Evicted expired recommendations for finderId={}", finderId);
            }
        }

        if (evictedCount > 0) {
            log.info("Evicted {} expired recommendation entries", evictedCount);
        }
    }

    /**
     * Checks if a specific entry has expired and removes it if so.
     *
     * @param finderId the Telegram user ID of the finder
     */
    private void cleanIfExpired(Long finderId) {
        Instant created = timestamps.get(finderId);
        if (created == null) {
            return;
        }

        if (Instant.now().isAfter(created.plus(TTL))) {
            cache.remove(finderId);
            timestamps.remove(finderId);

            log.debug("Cleaned expired entry for finderId={}", finderId);
        }
    }
}
