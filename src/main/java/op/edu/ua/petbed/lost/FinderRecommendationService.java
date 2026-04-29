package op.edu.ua.petbed.lost;

import org.jspecify.annotations.NullMarked;

import java.util.List;
import java.util.Optional;

/**
 * Public API for managing finder recommendations.
 * <p>
 * This service provides access to the recommendation cache for finders
 * who have submitted found pet requests. It allows retrieving potential
 * lost pet matches in FIFO order with TTL-based expiration.
 *
 * @see op.edu.ua.petbed.lost.domain.service.FinderRecommendationCache
 */
@NullMarked
public interface FinderRecommendationService {

    /**
     * Retrieves and removes the next recommendation from the queue.
     * Returns empty if no recommendations exist or if TTL has expired.
     *
     * @param finderId the Telegram user ID of the finder
     * @return the next lost request ID, or empty if none available
     */
    Optional<Long> pollNext(Long finderId);

    /**
     * Checks if the finder has any available recommendations.
     * Returns false if cache is empty or TTL has expired.
     *
     * @param finderId the Telegram user ID of the finder
     * @return true if there are recommendations available
     */
    boolean hasRecommendations(Long finderId);

    /**
     * Removes all recommendations for a finder.
     * Called when finder declines viewing or when cache expires.
     *
     * @param finderId the Telegram user ID of the finder
     */
    void remove(Long finderId);
}
