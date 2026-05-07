package op.edu.ua.petbed.pet;

import op.edu.ua.petbed.common.dto.CreatePetDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.dto.UpdatePetDTO;
import op.edu.ua.petbed.common.model.PetStatus;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@NullMarked
public interface PetService {

    PetDTO create(CreatePetDTO dto);

    PetDTO update(UpdatePetDTO dto);

    void delete(Long id);

    PetDTO findById(Long id);

    Page<PetDTO> findAllByOwnerId(Long ownerId, Pageable pageable);

    Page<PetDTO> findPetsAvailableForLostSearch(Long ownerId, Pageable pageable);

    void updateStatus(Long petId, PetStatus status);

    void updateSpecialFeatures(Long petId, String specialFeatures);

    void transferOwnership(Long petId, Long newOwnerId);
}