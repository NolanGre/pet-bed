package op.edu.ua.petbed.adoption.domain.repository;

import op.edu.ua.petbed.adoption.domain.model.AdoptionPost;
import op.edu.ua.petbed.common.model.AdoptionPostStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@NullMarked
public interface AdoptionPostRepository extends JpaRepository<AdoptionPost, Long> {

    @Nullable
    AdoptionPost findByPetId(Long petId);

    boolean existsByPetId(Long petId);

    @Query("SELECT ap FROM AdoptionPost ap WHERE ap.status = :status ORDER BY ap.createdAt DESC")
    List<AdoptionPost> findByStatus(@Param("status") AdoptionPostStatus status);

    @Query("SELECT ap FROM AdoptionPost ap WHERE ap.petId IN (SELECT p.id FROM Pet p WHERE p.ownerId = :ownerId) ORDER BY ap.createdAt DESC")
    List<AdoptionPost> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT ap FROM AdoptionPost ap WHERE ap.petId IN (SELECT p.id FROM Pet p WHERE p.ownerId = :ownerId) AND ap.status = :status ORDER BY ap.createdAt DESC")
    List<AdoptionPost> findByOwnerIdAndStatus(@Param("ownerId") Long ownerId, @Param("status") AdoptionPostStatus status);

    @Query("SELECT ap FROM AdoptionPost ap WHERE ap.petId IN (SELECT p.id FROM Pet p WHERE p.ownerId = :ownerId) ORDER BY ap.createdAt DESC")
    Page<AdoptionPost> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);

    /**
     * Finds unviewed active posts for a user, excluding their own posts.
     */
    @Query("SELECT ap FROM AdoptionPost ap WHERE ap.status = 'ACTIVE' " +
           "AND ap.petId NOT IN (SELECT p.id FROM Pet p WHERE p.ownerId = :userId) " +
           "AND ap.id NOT IN (SELECT avh.postId FROM AdoptionViewHistory avh WHERE avh.userId = :userId) " +
           "ORDER BY ap.createdAt DESC")
    Page<AdoptionPost> findUnviewedActivePosts(@Param("userId") Long userId, Pageable pageable);
}
