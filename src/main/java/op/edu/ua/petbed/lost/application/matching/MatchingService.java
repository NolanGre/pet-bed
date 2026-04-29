package op.edu.ua.petbed.lost.application.matching;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import op.edu.ua.petbed.lost.domain.repository.FoundRequestRepository;
import op.edu.ua.petbed.lost.domain.repository.LostRequestRepository;
import op.edu.ua.petbed.lost.domain.service.FinderRecommendationCache;
import op.edu.ua.petbed.lost.domain.service.MatchQueueService;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Service for processing matching between lost and found requests.
 * Handles asynchronous matching when new requests are created.
 *
 * <p>For new found requests:
 * <ul>
 *   <li>Finds all active lost requests of the same pet type within 50km</li>
 *   <li>Calculates matching scores for each pair</li>
 *   <li>Saves matches to match_queue for owners</li>
 *   <li>Populates finder cache with top 50 matches</li>
 * </ul>
 *
 * <p>For new lost requests:
 * <ul>
 *   <li>Finds recent found requests (last 30 days) of the same pet type within 50km</li>
 *   <li>Calculates matching scores for each pair</li>
 *   <li>Saves matches to match_queue</li>
 * </ul>
 */
@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchingService {

    private static final double MAX_DISTANCE_KM = 50.0;
    private static final int CACHE_SIZE_LIMIT = 50;
    private static final int LOST_REQUEST_DAYS_LOOKBACK = 30;

    private final MatchingAlgorithm matchingAlgorithm;
    private final LostRequestRepository lostRequestRepository;
    private final FoundRequestRepository foundRequestRepository;
    private final MatchQueueService matchQueueService;
    private final FinderRecommendationCache cache;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Processes matching for a newly created found request.
     * Finds all active lost requests of the same type within 50km radius,
     * calculates scores, and populates both match_queue and finder cache.
     *
     * <p>This method is async - it runs in a separate thread.
     *
     * @param foundRequestId the ID of the newly created found request
     */
    @Async
    public void processNewFoundRequest(Long foundRequestId) {
        log.info("Starting matching process for new found request: id={}", foundRequestId);

        try {
            processNewFoundRequestInternal(foundRequestId);
        } catch (Exception e) {
            log.error("Failed to process matching for found request id={}", foundRequestId, e);
            // Don't rethrow - async method should not propagate exceptions
        }
    }

    /**
     * Internal transactional method for processing found request matching.
     * Separated from @Async method to ensure proper transaction management.
     *
     * @param foundRequestId the ID of the found request
     */
    @Transactional
    void processNewFoundRequestInternal(Long foundRequestId) {
        FoundRequest found = foundRequestRepository.findById(foundRequestId)
                .orElseThrow(() -> new PetBedException(
                        "Found request not found: " + foundRequestId,
                        PetBedException.ErrorCode.FOUND_REQUEST_NOT_FOUND));

        // Find all active lost requests of the same pet type within 50km radius
        List<LostRequest> candidates = findActiveLostRequestsWithinRadius(
                found.getPetType(),
                found.getLocation()
        );

        log.info("Found {} candidate lost requests for found request id={}",
                candidates.size(), foundRequestId);

        // Calculate scores and filter out zero scores
        List<MatchResult> results = candidates.stream()
                .map(lost -> {
                    BigDecimal score = matchingAlgorithm.calculateScore(lost, found);
                    return new MatchResult(lost, found, score);
                })
                .filter(r -> r.score().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(MatchResult::score).reversed())
                .toList();

        log.info("Calculated {} valid matches (score > 0) for found request id={}",
                results.size(), foundRequestId);

        // Save matches to match_queue for owners
        results.forEach(r -> matchQueueService.addMatch(r.lost(), r.found(), r.score()));

        // Populate cache for finder (top 50 matches)
        List<Long> lostIds = results.stream()
                .limit(CACHE_SIZE_LIMIT)
                .map(r -> r.lost().getIdOrThrow())
                .toList();

        cache.put(found.getFinderId(), lostIds);

        log.info("Completed matching for found request id={}. Saved {} matches to queue, {} to cache.",
                foundRequestId, results.size(), lostIds.size());
    }

    /**
     * Processes matching for a newly created lost request.
     * Finds recent found requests (last 30 days) of the same type within 50km radius
     * and saves matches to match_queue.
     *
     * <p>This method is async - it runs in a separate thread.
     *
     * @param lostRequestId the ID of the newly created lost request
     */
    @Async
    public void processNewLostRequest(Long lostRequestId) {
        log.info("Starting matching process for new lost request: id={}", lostRequestId);

        try {
            processNewLostRequestInternal(lostRequestId);
        } catch (Exception e) {
            log.error("Failed to process matching for lost request id={}", lostRequestId, e);
            // Don't rethrow - async method should not propagate exceptions
        }
    }

    /**
     * Internal transactional method for processing lost request matching.
     * Separated from @Async method to ensure proper transaction management.
     *
     * @param lostRequestId the ID of the lost request
     */
    @Transactional
    void processNewLostRequestInternal(Long lostRequestId) {
        LostRequest lost = lostRequestRepository.findById(lostRequestId)
                .orElseThrow(() -> new PetBedException(
                        "Lost request not found: " + lostRequestId,
                        PetBedException.ErrorCode.LOST_REQUEST_NOT_FOUND));

        // Find recent found requests (last 30 days) of the same pet type within 50km
        Timestamp since = Timestamp.from(Instant.now().minus(Duration.ofDays(LOST_REQUEST_DAYS_LOOKBACK)));
        List<FoundRequest> candidates = foundRequestRepository.findRecentByPetTypeAndLocation(
                lost.getPetType().name(),
                lost.getLastSeenLocation(),
                since
        );

        log.info("Found {} candidate found requests for lost request id={}",
                candidates.size(), lostRequestId);

        // Calculate scores and save matches
        candidates.stream()
                .map(found -> {
                    BigDecimal score = matchingAlgorithm.calculateScore(lost, found);
                    return new MatchResult(lost, found, score);
                })
                .filter(r -> r.score().compareTo(BigDecimal.ZERO) > 0)
                .forEach(r -> matchQueueService.addMatch(r.lost(), r.found(), r.score()));

        log.info("Completed matching for lost request id={}", lostRequestId);
    }

    /**
     * Finds all active lost requests of a given pet type within 50km radius.
     * Uses native PostGIS query for efficient geographic filtering, then batch loads
     * entities via repository to avoid N+1 query problem.
     *
     * @param petType  the pet type to filter by
     * @param location the center point for radius search
     * @return list of matching lost requests
     */
    private List<LostRequest> findActiveLostRequestsWithinRadius(PetType petType, Point location) {
        String sql = """
                SELECT lr.id FROM lost_requests lr
                WHERE lr.pet_type = ?
                AND lr.status = 'ACTIVE'
                AND ST_DWithin(lr.last_seen_location, ?::geography, ?)
                """;

        List<Long> ids = jdbcTemplate.queryForList(sql, Long.class,
                petType.name(), toWkt(location), MAX_DISTANCE_KM * 1000);

        // Batch load all entities to avoid N+1 query problem
        return lostRequestRepository.findAllById(ids);
    }

    /**
     * Converts a JTS Point to WKT (Well-Known Text) format for PostGIS.
     * Format: SRID=4326;POINT(x y)
     *
     * @param point the point to convert
     * @return WKT string representation
     */
    private String toWkt(Point point) {
        return String.format("SRID=4326;POINT(%f %f)", point.getX(), point.getY());
    }

    /**
     * Record representing a match result between a lost and found request.
     */
    private record MatchResult(LostRequest lost, FoundRequest found, BigDecimal score) {
    }
}
