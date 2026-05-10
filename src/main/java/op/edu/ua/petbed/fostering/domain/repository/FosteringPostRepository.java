package op.edu.ua.petbed.fostering.domain.repository;

import op.edu.ua.petbed.fostering.domain.model.FosteringPost;
import op.edu.ua.petbed.fostering.domain.model.FosteringPostStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

@NullMarked
public interface FosteringPostRepository extends JpaRepository<FosteringPost, Long> {

    @Nullable
    FosteringPost findByPetId(Long petId);

    boolean existsByPetId(Long petId);

    @Query("SELECT fp FROM FosteringPost fp WHERE fp.status = :status ORDER BY fp.createdAt DESC")
    List<FosteringPost> findByStatus(@Param("status") FosteringPostStatus status);

    @Query("SELECT fp FROM FosteringPost fp WHERE fp.petId IN (SELECT p.id FROM Pet p WHERE p.ownerId = :ownerId) ORDER BY fp.createdAt DESC")
    List<FosteringPost> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT fp FROM FosteringPost fp WHERE fp.petId IN (SELECT p.id FROM Pet p WHERE p.ownerId = :ownerId) AND fp.status = :status ORDER BY fp.createdAt DESC")
    List<FosteringPost> findByOwnerIdAndStatus(@Param("ownerId") Long ownerId, @Param("status") FosteringPostStatus status);

    @Query("SELECT fp FROM FosteringPost fp WHERE fp.petId IN (SELECT p.id FROM Pet p WHERE p.ownerId = :ownerId) ORDER BY fp.createdAt DESC")
    Page<FosteringPost> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    @Query("SELECT COUNT(fp) > 0 FROM FosteringPost fp WHERE fp.petId IN (SELECT p.id FROM Pet p WHERE p.ownerId = :ownerId)")
    boolean existsByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT fp FROM FosteringPost fp WHERE fp.status = 'ACTIVE' " +
           "AND fp.petId NOT IN (SELECT p.id FROM Pet p WHERE p.ownerId = :userId) " +
           "AND fp.id NOT IN (SELECT fvh.postId FROM FosteringViewHistory fvh WHERE fvh.userId = :userId) " +
           "ORDER BY fp.createdAt DESC")
    Page<FosteringPost> findUnviewedActivePosts(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT fp FROM FosteringPost fp WHERE fp.status = 'COMPLETED' AND fp.expiresAt <= :now")
    List<FosteringPost> findExpired(@Param("now") Instant now);
}
