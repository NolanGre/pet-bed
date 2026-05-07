package op.edu.ua.petbed.adoption;

import op.edu.ua.petbed.adoption.application.dto.AdoptionPostDTO;
import org.jspecify.annotations.NullMarked;

import java.util.List;

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
     * Finds all saved adoption posts for a user.
     *
     * @param userId the user ID
     * @return list of saved adoption post DTOs
     */
    List<AdoptionPostDTO> findSavedByUserId(Long userId);

    /**
     * Checks if a user has saved a specific post.
     *
     * @param postId the adoption post ID
     * @param userId the user ID
     * @return true if the post is saved
     */
    boolean isSaved(Long postId, Long userId);
}
