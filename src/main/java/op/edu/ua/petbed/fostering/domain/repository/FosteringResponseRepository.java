package op.edu.ua.petbed.fostering.domain.repository;

import op.edu.ua.petbed.fostering.domain.model.FosteringResponse;
import op.edu.ua.petbed.fostering.domain.model.FosteringResponseStatus;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@NullMarked
public interface FosteringResponseRepository extends JpaRepository<FosteringResponse, Long> {

    List<FosteringResponse> findByFosteringPostId(Long postId);

    List<FosteringResponse> findByFosteringPostIdAndStatus(Long postId, FosteringResponseStatus status);

    List<FosteringResponse> findByResponderId(Long responderId);

    Page<FosteringResponse> findByResponderId(Long responderId, Pageable pageable);

    boolean existsByFosteringPostIdAndResponderId(Long postId, Long responderId);

    boolean existsByResponderId(Long responderId);

    @Query("SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END FROM FosteringResponse r " +
           "WHERE r.fosteringPostId = :postId AND r.responderId = :responderId " +
           "AND r.status IN ('NEW', 'CONFIRMED_BY_OWNER')")
    boolean existsActiveByFosteringPostIdAndResponderId(Long postId, Long responderId);

    Optional<FosteringResponse> findByFosteringPostIdAndResponderId(Long postId, Long responderId);

    @Modifying
    @Query("UPDATE FosteringResponse fr SET fr.status = 'REJECTED_BY_OWNER' " +
           "WHERE fr.fosteringPostId = :postId AND fr.id != :confirmedId AND fr.status = 'NEW'")
    void rejectOthers(@Param("postId") Long postId, @Param("confirmedId") Long confirmedId);

    @Query("SELECT fr FROM FosteringResponse fr WHERE fr.fosteringPostId = :postId " +
           "ORDER BY CASE fr.status " +
           "WHEN 'CONFIRMED_BY_OWNER' THEN 1 " +
           "WHEN 'NEW' THEN 2 " +
           "WHEN 'REJECTED_BY_OWNER' THEN 3 " +
           "ELSE 4 END, fr.createdAt DESC")
    List<FosteringResponse> findByPostIdOrderByStatusPriority(@Param("postId") Long postId);

    @Query(value = "SELECT fr FROM FosteringResponse fr WHERE fr.fosteringPostId = :postId " +
           "ORDER BY CASE fr.status " +
           "WHEN 'CONFIRMED_BY_OWNER' THEN 1 " +
           "WHEN 'NEW' THEN 2 " +
           "WHEN 'REJECTED_BY_OWNER' THEN 3 " +
           "ELSE 4 END, fr.createdAt DESC",
           countQuery = "SELECT COUNT(fr) FROM FosteringResponse fr WHERE fr.fosteringPostId = :postId")
    Page<FosteringResponse> findByPostIdOrderByStatusPriority(@Param("postId") Long postId, Pageable pageable);

    @Modifying
    void deleteByFosteringPostId(Long postId);
}
