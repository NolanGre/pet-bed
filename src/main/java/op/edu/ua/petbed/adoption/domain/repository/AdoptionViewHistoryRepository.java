package op.edu.ua.petbed.adoption.domain.repository;

import op.edu.ua.petbed.adoption.domain.model.AdoptionViewHistory;
import op.edu.ua.petbed.adoption.domain.model.AdoptionViewHistoryId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@NullMarked
@Repository
public interface AdoptionViewHistoryRepository extends JpaRepository<AdoptionViewHistory, AdoptionViewHistoryId> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    List<AdoptionViewHistory> findByUserId(Long userId);

    @Modifying
    @Query("DELETE FROM AdoptionViewHistory avh WHERE avh.userId = :userId")
    void deleteByUserId(@Param("userId") Long userId);

    void deleteByPostId(Long postId);
}
