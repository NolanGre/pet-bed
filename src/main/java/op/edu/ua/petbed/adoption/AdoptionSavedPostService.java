package op.edu.ua.petbed.adoption;

import op.edu.ua.petbed.common.dto.AdoptionPostDTO;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public API for managing saved adoption posts.
 * This interface is part of the Spring Modulith public API.
 */
@NullMarked
public interface AdoptionSavedPostService {

    /**
     * Saves an adoption post for a user.
     *
     * @param postId the adoption post ID
     * @param userId the user ID
     */
    void save(Long postId, Long userId);

    /**
     * Removes a saved adoption post for a user.
     *
     * @param postId the adoption post ID
     * @param userId the user ID
     */
    void unsave(Long postId, Long userId);

    /**
     * Finds saved adoption posts for a user with pagination.
     *
     * @param userId   the user ID
     * @param pageable the pagination parameters
     * @return page of saved adoption post DTOs
     */
    Page<AdoptionPostDTO> findSavedByUserId(Long userId, Pageable pageable);

    /**
     * Checks if a user has saved a specific post.
     *
     * @param postId the adoption post ID
     * @param userId the user ID
     * @return true if the post is saved
     */
    boolean isSaved(Long postId, Long userId);

    /**
     * Checks if a user has any saved posts.
     *
     * @param userId the user ID
     * @return true if the user has at least one saved post
     */
    boolean existsByUserId(Long userId);
}
