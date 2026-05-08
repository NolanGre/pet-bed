package op.edu.ua.petbed.adoption.domain.repository;

import op.edu.ua.petbed.adoption.domain.model.AdoptionSavedPost;
import op.edu.ua.petbed.adoption.domain.model.AdoptionSavedPostId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@NullMarked
@Repository
public interface AdoptionSavedPostRepository extends JpaRepository<AdoptionSavedPost, AdoptionSavedPostId> {

    @Query("SELECT s FROM AdoptionSavedPost s JOIN FETCH AdoptionPost p WHERE s.userId = :userId AND p.id = s.postId")
    List<AdoptionSavedPost> findByUserId(@Param("userId") Long userId);

    @Query(value = "SELECT s FROM AdoptionSavedPost s JOIN AdoptionPost p ON p.id = s.postId WHERE s.userId = :userId",
           countQuery = "SELECT COUNT(s) FROM AdoptionSavedPost s WHERE s.userId = :userId")
    Page<AdoptionSavedPost> findByUserId(@Param("userId") Long userId, Pageable pageable);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    boolean existsByUserId(Long userId);

    void deleteByPostIdAndUserId(Long postId, Long userId);

    void deleteByPostId(Long postId);
}
