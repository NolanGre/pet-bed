package op.edu.ua.petbed.fostering.domain.repository;

import op.edu.ua.petbed.fostering.domain.model.FosteringViewHistory;
import op.edu.ua.petbed.fostering.domain.model.FosteringViewHistoryId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@NullMarked
public interface FosteringViewHistoryRepository extends JpaRepository<FosteringViewHistory, FosteringViewHistoryId> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    List<FosteringViewHistory> findByUserIdOrderByViewedAtDesc(Long userId);

    List<FosteringViewHistory> findByUserIdOrderByViewedAtDesc(Long userId, Pageable pageable);

    @Modifying
    void deleteByPostId(Long postId);
}
