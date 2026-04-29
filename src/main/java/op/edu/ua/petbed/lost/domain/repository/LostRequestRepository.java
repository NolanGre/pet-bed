package op.edu.ua.petbed.lost.domain.repository;

import op.edu.ua.petbed.lost.domain.model.LostRequest;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

@NullMarked
public interface LostRequestRepository extends JpaRepository<LostRequest, Long> {

    @Nullable LostRequest findByPetId(Long petId);

    boolean existsByPetId(Long petId);

    @Modifying
    @Query("DELETE FROM LostRequest lr WHERE lr.petId = :petId")
    void deleteByPetId(@Param("petId") Long petId);

    @Query("SELECT lr FROM LostRequest lr JOIN Pet p ON lr.petId = p.id WHERE p.ownerId = :ownerId")
    Page<LostRequest> findByOwnerId(@Param("ownerId") Long ownerId, Pageable pageable);
}
