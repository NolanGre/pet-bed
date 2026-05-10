package op.edu.ua.petbed.fostering;

import op.edu.ua.petbed.common.dto.FosteringPostDTO;
import op.edu.ua.petbed.common.dto.FosteringPostDetailDTO;
import op.edu.ua.petbed.common.dto.FosteringRecommendationDTO;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public API for managing fostering posts.
 * This interface is part of the Spring Modulith public API.
 */
@NullMarked
public interface FosteringPostService {

    FosteringPostDTO create(Long petId, Integer plannedDurationDays, @Nullable String ownerComment);

    FosteringPostDetailDTO findById(Long id);

    Page<FosteringPostDTO> findAllByOwnerId(Long ownerId, Pageable pageable);

    void cancel(Long postId, Long ownerId);

    boolean existsByPetId(Long petId);

    boolean existsByOwnerId(Long ownerId);

    @Nullable
    FosteringRecommendationDTO findNextUnviewed(Long userId);

    void recordView(Long postId, Long userId);

    @Nullable
    FosteringRecommendationDTO findPreviousFromHistory(Long userId);

    @Nullable
    FosteringRecommendationDTO findByIdAsRecommendation(Long postId);
}
