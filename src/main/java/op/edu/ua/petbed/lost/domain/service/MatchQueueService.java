package op.edu.ua.petbed.lost.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.lost.MatchRecommendationDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import op.edu.ua.petbed.lost.domain.model.MatchQueueEntry;
import op.edu.ua.petbed.lost.domain.model.ViewingStatus;
import op.edu.ua.petbed.lost.domain.repository.MatchQueueRepository;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Service for managing match queue entries between lost and found requests.
 * Handles creation of matches, retrieval of recommendations, and status updates.
 */
@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class MatchQueueService implements op.edu.ua.petbed.lost.MatchQueueQueryService {

    private final MatchQueueRepository matchQueueRepository;
    private final JdbcTemplate jdbcTemplate;

    /**
     * Adds a new match between a lost request and a found request if it doesn't already exist.
     *
     * @param lost  the lost request
     * @param found the found request
     * @param score the matching score (0.0000 - 1.0000)
     */
    @Transactional
    public void addMatch(LostRequest lost, FoundRequest found, BigDecimal score) {
        Long lostRequestId = lost.getIdOrThrow();
        Long foundRequestId = found.getIdOrThrow();

        boolean exists = matchQueueRepository.existsByLostRequestIdAndFoundRequestId(lostRequestId, foundRequestId);
        if (exists) {
            log.debug("Match already exists for lostRequestId={} and foundRequestId={}", lostRequestId, foundRequestId);
            return;
        }

        MatchQueueEntry entry = MatchQueueEntry.create(lost, found, score);
        MatchQueueEntry saved = matchQueueRepository.save(entry);

        log.info("Created match queue entry: id={}, lostRequestId={}, foundRequestId={}, score={}",
                saved.getIdOrThrow(), lostRequestId, foundRequestId, score);
    }

    /**
     * Retrieves match recommendations for the owner of a lost request.
     * Returns entries with NEW or VIEWED status, sorted by score descending.
     *
     * @param lostRequestId the ID of the lost request
     * @return list of match recommendations
     */
    @Transactional(readOnly = true)
    public List<MatchRecommendationDTO> getRecommendationsForOwner(Long lostRequestId) {
        List<ViewingStatus> statuses = List.of(ViewingStatus.NEW, ViewingStatus.VIEWED);
        List<MatchQueueEntry> entries = matchQueueRepository.findByLostRequestIdAndViewingStatusIn(lostRequestId, statuses);

        return entries.stream()
                .sorted((e1, e2) -> e2.getScore().compareTo(e1.getScore()))
                .map(this::mapToDTO)
                .toList();
    }

    /**
     * Marks a match queue entry as viewed by the owner.
     *
     * @param matchQueueId the ID of the match queue entry
     */
    @Transactional
    public void markAsViewed(Long matchQueueId) {
        MatchQueueEntry entry = matchQueueRepository.findById(matchQueueId)
                .orElseThrow(() -> new PetBedException(
                        "Match queue entry not found with id: " + matchQueueId,
                        PetBedException.ErrorCode.MATCH_QUEUE_ENTRY_NOT_PERSISTED));

        entry.markAsViewed();
        matchQueueRepository.save(entry);

        log.info("Marked match queue entry as viewed: id={}", matchQueueId);
    }

    /**
     * Marks a match queue entry as confirmed.
     *
     * @param matchQueueId the ID of the match queue entry
     */
    @Transactional
    public void markAsConfirmed(Long matchQueueId) {
        MatchQueueEntry entry = matchQueueRepository.findById(matchQueueId)
                .orElseThrow(() -> new PetBedException(
                        "Match queue entry not found with id: " + matchQueueId,
                        PetBedException.ErrorCode.MATCH_QUEUE_ENTRY_NOT_PERSISTED));

        entry.markAsConfirmed();
        matchQueueRepository.save(entry);

        log.info("Marked match queue entry as confirmed: id={}", matchQueueId);
    }

    /**
     * Gets the next recommendation for the owner and marks it as viewed.
     * Returns the first NEW entry sorted by score descending.
     *
     * @param lostRequestId the ID of the lost request
     * @return the next recommendation, or empty if none available
     */
    @Transactional
    public java.util.Optional<MatchRecommendationDTO> getNextRecommendation(Long lostRequestId) {
        List<ViewingStatus> statuses = List.of(ViewingStatus.NEW);
        List<MatchQueueEntry> entries = matchQueueRepository.findByLostRequestIdAndViewingStatusIn(lostRequestId, statuses);

        return entries.stream()
                .sorted((e1, e2) -> e2.getScore().compareTo(e1.getScore()))
                .findFirst()
                .map(entry -> {
                    entry.markAsViewed();
                    matchQueueRepository.save(entry);
                    log.info("Marked match queue entry as viewed: id={}", entry.getIdOrThrow());
                    return mapToDTO(entry);
                });
    }

    /**
     * Finds a match queue entry by ID.
     *
     * @param matchQueueId the ID of the match queue entry
     * @return the match recommendation DTO, or empty if not found
     */
    @Override
    @Transactional(readOnly = true)
    public java.util.Optional<MatchRecommendationDTO> findById(Long matchQueueId) {
        return matchQueueRepository.findById(matchQueueId)
                .map(this::mapToDTO);
    }

    /**
     * Maps a MatchQueueEntry to a MatchRecommendationDTO including distance calculation.
     *
     * @param entry the match queue entry
     * @return the DTO representation
     */
    private MatchRecommendationDTO mapToDTO(MatchQueueEntry entry) {
        LostRequest lostRequest = entry.getLostRequest();
        FoundRequest foundRequest = entry.getFoundRequest();

        Double distanceKm = calculateDistance(lostRequest.getLastSeenLocation(), foundRequest.getLocation());

        return new MatchRecommendationDTO(
                entry.getIdOrThrow(),
                lostRequest.getIdOrThrow(),
                foundRequest.getIdOrThrow(),
                entry.getScore(),
                foundRequest.getPhotoUrl(),
                foundRequest.getDescription(),
                foundRequest.getLocation(),
                Objects.requireNonNull(foundRequest.getCreatedAt()),
                distanceKm
        );
    }

    /**
     * Calculates the distance between two geographic points using PostGIS.
     *
     * @param loc1 the first location point
     * @param loc2 the second location point
     * @return distance in kilometers, or 0.0 if calculation fails
     */
    private Double calculateDistance(Point loc1, Point loc2) {
        String sql = "SELECT ST_Distance(?::geography, ?::geography) / 1000.0";
        Double result = jdbcTemplate.queryForObject(sql, Double.class, loc1.toString(), loc2.toString());
        return result != null ? result : 0.0;
    }
}
