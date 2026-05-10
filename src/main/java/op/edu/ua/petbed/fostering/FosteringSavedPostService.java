package op.edu.ua.petbed.fostering;

import op.edu.ua.petbed.common.dto.FosteringPostDTO;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Public API for managing saved fostering posts.
 * This interface is part of the Spring Modulith public API.
 */
@NullMarked
public interface FosteringSavedPostService {

    void save(Long postId, Long userId);

    void unsave(Long postId, Long userId);

    Page<FosteringPostDTO> findSavedByUserId(Long userId, Pageable pageable);

    boolean isSaved(Long postId, Long userId);

    boolean existsByUserId(Long userId);
}
