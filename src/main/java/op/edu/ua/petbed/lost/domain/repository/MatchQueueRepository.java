package op.edu.ua.petbed.lost.domain.repository;

import op.edu.ua.petbed.lost.domain.model.MatchQueueEntry;
import op.edu.ua.petbed.lost.domain.model.ViewingStatus;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

@NullMarked
public interface MatchQueueRepository extends JpaRepository<MatchQueueEntry, Long> {

    List<MatchQueueEntry> findByLostRequestIdAndViewingStatusIn(Long lostRequestId, List<ViewingStatus> statuses);

    List<MatchQueueEntry> findByLostRequestIdOrderByScoreDesc(Long lostRequestId);

    boolean existsByLostRequestIdAndFoundRequestId(Long lostRequestId, Long foundRequestId);

    @Modifying
    void deleteByLostRequestId(Long lostRequestId);
}
