package op.edu.ua.petbed.fostering.domain.repository;

import op.edu.ua.petbed.fostering.domain.model.FosteringSavedPost;
import op.edu.ua.petbed.fostering.domain.model.FosteringSavedPostId;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@NullMarked
public interface FosteringSavedPostRepository extends JpaRepository<FosteringSavedPost, FosteringSavedPostId> {

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    boolean existsByUserId(Long userId);

    List<FosteringSavedPost> findByUserId(Long userId);

    Page<FosteringSavedPost> findByUserId(Long userId, Pageable pageable);

    @Query("SELECT fp.id FROM FosteringPost fp WHERE fp.id IN " +
           "(SELECT fsp.postId FROM FosteringSavedPost fsp WHERE fsp.userId = :userId) " +
           "ORDER BY fp.createdAt DESC")
    List<Long> findPostIdsByUserId(@Param("userId") Long userId);

    @Modifying
    void deleteByPostId(Long postId);
}
