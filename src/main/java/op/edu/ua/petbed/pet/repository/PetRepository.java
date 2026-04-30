package op.edu.ua.petbed.pet.repository;

import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.pet.model.Pet;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

@NullMarked
public interface PetRepository extends JpaRepository<Pet, Long> {

    Page<Pet> findByOwnerIdOrderByUpdatedAtDesc(Long ownerId, Pageable pageable);

    Page<Pet> findByOwnerIdAndStatusOrderByUpdatedAtDesc(Long ownerId, PetStatus status, Pageable pageable);
}