package op.edu.ua.petbed.fostering;

import op.edu.ua.petbed.common.dto.FosteringResponseDTO;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public API for managing fostering responses.
 * This interface is part of the Spring Modulith public API.
 */
@NullMarked
public interface FosteringResponseService {

    FosteringResponseDTO create(Long postId, Long responderId, String comment);

    Page<FosteringResponseDTO> findByPostId(Long postId, Pageable pageable);

    FosteringResponseDTO findById(Long id);

    void confirmByOwner(Long responseId, Long ownerId);

    void rejectByOwner(Long responseId, Long ownerId);

    void restore(Long responseId, Long ownerId);

    void finalConfirm(Long responseId, Long responderId);

    void declineFinalization(Long responseId, Long responderId);

    boolean hasResponded(Long postId, Long userId);

    boolean hasActiveResponse(Long postId, Long userId);

    Page<FosteringResponseDTO> findByResponderId(Long responderId, Pageable pageable);

    boolean existsByResponderId(Long responderId);
}
