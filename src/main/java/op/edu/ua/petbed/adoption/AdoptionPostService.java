package op.edu.ua.petbed.adoption;

import op.edu.ua.petbed.common.dto.AdoptionPostDTO;
import op.edu.ua.petbed.common.dto.AdoptionPostDetailDTO;
import op.edu.ua.petbed.common.dto.AdoptionRecommendationDTO;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

/**
 * Public API for managing adoption posts.
 * This interface is part of the Spring Modulith public API.
 */
@NullMarked
public interface AdoptionPostService {

    /**
     * Creates a new adoption post for a pet.
     *
     * @param petId the ID of the pet to put up for adoption
     * @param ownerComment optional comment from the owner
     * @return the created adoption post DTO
     * @throws op.edu.ua.petbed.common.exceptions.PetBedException if pet already has an active adoption post
     */
    AdoptionPostDTO create(Long petId, @Nullable String ownerComment);

    /**
     * Finds an adoption post by its ID.
     *
     * @param id the adoption post ID
     * @return the adoption post details with responses
     */
    AdoptionPostDetailDTO findById(Long id);

    /**
     * Finds adoption posts for a specific owner with pagination.
     *
     * @param ownerId the owner user ID
     * @param pageable pagination parameters
     * @return page of adoption post DTOs
     */
    Page<AdoptionPostDTO> findAllByOwnerId(Long ownerId, Pageable pageable);

    /**
     * Cancels an active adoption post.
     *
     * @param postId the adoption post ID
     * @param ownerId the owner user ID (for authorization)
     * @throws op.edu.ua.petbed.common.exceptions.PetBedException if post not found or not owned by user
     */
    void cancel(Long postId, Long ownerId);

    /**
     * Checks if a pet already has an active adoption post.
     *
     * @param petId the pet ID
     * @return true if an active post exists
     */
    boolean existsByPetId(Long petId);

    // Feed methods

    /**
     * Finds the next adoption post for the user's feed.
     *
     * @param userId the user ID
     * @param offset the current offset
     * @return the next recommendation, or null if no more posts
     */
    @Nullable
    AdoptionRecommendationDTO findNextForFeed(Long userId, int offset);

    /**
     * Records that a user has viewed a post.
     *
     * @param postId the post ID
     * @param userId the user ID
     */
    void recordView(Long postId, Long userId);

    /**
     * Gets the current feed offset for a user.
     *
     * @param userId the user ID
     * @return the current offset
     */
    int getOffset(Long userId);

    /**
     * Resets the feed offset for a user.
     *
     * @param userId the user ID
     */
    void resetOffset(Long userId);
}
