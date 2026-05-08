package op.edu.ua.petbed.adoption.domain.repository;

import op.edu.ua.petbed.adoption.domain.model.AdoptionResponse;
import op.edu.ua.petbed.adoption.domain.model.AdoptionResponseStatus;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@NullMarked
public interface AdoptionResponseRepository extends JpaRepository<AdoptionResponse, Long> {

    List<AdoptionResponse> findByAdoptionPostId(Long postId);

    List<AdoptionResponse> findByAdoptionPostIdAndStatus(Long postId, AdoptionResponseStatus status);

    List<AdoptionResponse> findByResponderId(Long responderId);

    boolean existsByAdoptionPostIdAndResponderId(Long postId, Long responderId);

    Optional<AdoptionResponse> findByAdoptionPostIdAndResponderId(Long postId, Long responderId);

    /**
     * Rejects all other responses for the same post.
     */
    @Modifying
    @Query("UPDATE AdoptionResponse ar SET ar.status = 'REJECTED_BY_OWNER' " +
           "WHERE ar.adoptionPostId = :postId AND ar.id != :confirmedId AND ar.status = 'NEW'")
    void rejectOthers(@Param("postId") Long postId, @Param("confirmedId") Long confirmedId);

    /**
     * Finds responses sorted by status priority: CONFIRMED_BY_OWNER > NEW > REJECTED_BY_OWNER > others.
     */
    @Query("SELECT ar FROM AdoptionResponse ar WHERE ar.adoptionPostId = :postId " +
           "ORDER BY CASE ar.status " +
           "WHEN 'CONFIRMED_BY_OWNER' THEN 1 " +
           "WHEN 'NEW' THEN 2 " +
           "WHEN 'REJECTED_BY_OWNER' THEN 3 " +
           "ELSE 4 END, ar.createdAt DESC")
    List<AdoptionResponse> findByPostIdOrderByStatusPriority(@Param("postId") Long postId);
}
