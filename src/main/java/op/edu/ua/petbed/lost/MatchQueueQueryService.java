package op.edu.ua.petbed.lost;

import op.edu.ua.petbed.lost.MatchRecommendationDTO;
import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;

/**
 * Public API for querying match queue entries.
 * Used by other modules (e.g., telegram) to retrieve recommendations.
 */
@NullMarked
public interface MatchQueueQueryService {

    /**
     * Retrieves all match recommendations for the owner of a lost request.
     * Returns entries with NEW or VIEWED status, sorted by score descending.
     *
     * @param lostRequestId the ID of the lost request
     * @return list of match recommendations
     */
    List<MatchRecommendationDTO> getRecommendationsForOwner(Long lostRequestId);

    /**
     * Gets the next recommendation for the owner and marks it as viewed.
     * Returns the first NEW or VIEWED entry sorted by score descending.
     *
     * @param lostRequestId the ID of the lost request
     * @return the next recommendation, or empty if none available
     */
    Optional<MatchRecommendationDTO> getNextRecommendation(Long lostRequestId);

    /**
     * Finds a match queue entry by ID.
     *
     * @param matchQueueId the ID of the match queue entry
     * @return the match recommendation DTO, or empty if not found
     */
    Optional<MatchRecommendationDTO> findById(Long matchQueueId);

    /**
     * Marks a match queue entry as confirmed.
     *
     * @param matchQueueId the ID of the match queue entry
     */
    void markAsConfirmed(Long matchQueueId);
}