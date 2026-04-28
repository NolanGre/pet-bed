package op.edu.ua.petbed.lost.domain.repository;

import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import op.edu.ua.petbed.lost.domain.model.LostRequestStatus;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

@NullMarked
public interface LostRequestRepository extends JpaRepository<LostRequest, Long> {

    Optional<LostRequest> findByPetId(Long petId);

    List<LostRequest> findByPetOwnerIdAndStatus(Long ownerId, LostRequestStatus status);

    List<LostRequest> findByStatusAndPetType(LostRequestStatus status, PetType petType);

    boolean existsByPetId(Long petId);

    @Modifying
    @Query("DELETE FROM LostRequest lr WHERE lr.pet.id = :petId")
    void deleteByPetId(@Param("petId") Long petId);
}
