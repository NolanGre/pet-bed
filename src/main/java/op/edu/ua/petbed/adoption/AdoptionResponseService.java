package op.edu.ua.petbed.adoption;

import op.edu.ua.petbed.common.dto.AdoptionResponseDTO;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public API for managing adoption responses.
 * This interface is part of the Spring Modulith public API.
 */
@NullMarked
public interface AdoptionResponseService {

    /**
     * Creates a new response to an adoption post.
     *
     * @param postId the adoption post ID
     * @param responderId the responding user's ID
     * @param comment the response comment
     * @return the created response DTO
     * @throws op.edu.ua.petbed.common.exceptions.PetBedException if user already responded to this post
     */
    AdoptionResponseDTO create(Long postId, Long responderId, String comment);

    /**
     * Finds responses for a specific adoption post with pagination.
     *
     * @param postId the adoption post ID
     * @param pageable pagination parameters
     * @return page of response DTOs sorted by status priority
     */
    Page<AdoptionResponseDTO> findByPostId(Long postId, Pageable pageable);

    /**
     * Finds a response by its ID.
     *
     * @param id the response ID
     * @return the response DTO
     */
    AdoptionResponseDTO findById(Long id);

    /**
     * Confirms a response by the post owner.
     * Automatically rejects all other responses for the same post.
     *
     * @param responseId the response ID to confirm
     * @param ownerId the owner's user ID (for authorization)
     * @throws op.edu.ua.petbed.common.exceptions.PetBedException if response not found or not authorized
     */
    void confirmByOwner(Long responseId, Long ownerId);

    /**
     * Rejects a response by the post owner.
     *
     * @param responseId the response ID to reject
     * @param ownerId the owner's user ID (for authorization)
     * @throws op.edu.ua.petbed.common.exceptions.PetBedException if response not found or not authorized
     */
    void rejectByOwner(Long responseId, Long ownerId);

    /**
     * Restores a previously rejected response to NEW status.
     *
     * @param responseId the response ID to restore
     * @param ownerId the owner's user ID (for authorization)
     * @throws op.edu.ua.petbed.common.exceptions.PetBedException if response not found or not authorized
     */
    void restore(Long responseId, Long ownerId);

    /**
     * Final confirmation by the responder to complete the adoption.
     *
     * @param responseId the response ID
     * @param responderId the responder's user ID (for authorization)
     * @throws op.edu.ua.petbed.common.exceptions.PetBedException if response not confirmed by owner
     */
    void finalConfirm(Long responseId, Long responderId);

    /**
     * Declines finalization by the responder.
     *
     * @param responseId the response ID
     * @param responderId the responder's user ID (for authorization)
     * @throws op.edu.ua.petbed.common.exceptions.PetBedException if response not found or not authorized
     */
    void declineFinalization(Long responseId, Long responderId);

    /**
     * Checks if a user has already responded to a post.
     *
     * @param postId the adoption post ID
     * @param userId the user ID
     * @return true if the user has already responded
     */
    boolean hasResponded(Long postId, Long userId);

    /**
     * Finds responses by a specific user (responder) with pagination.
     *
     * @param responderId the responder's user ID
     * @param pageable pagination parameters
     * @return page of response DTOs sorted by creation date
     */
    Page<AdoptionResponseDTO> findByResponderId(Long responderId, Pageable pageable);

    /**
     * Checks if a user has any responses.
     *
     * @param responderId the responder's user ID
     * @return true if the user has at least one response
     */
    boolean existsByResponderId(Long responderId);
}
