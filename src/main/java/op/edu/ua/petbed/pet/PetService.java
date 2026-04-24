package op.edu.ua.petbed.pet;

import op.edu.ua.petbed.common.dto.CreatePetDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.dto.UpdatePetDTO;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@NullMarked
public interface PetService {

    PetDTO create(CreatePetDTO dto);

    PetDTO update(UpdatePetDTO dto);

    void delete(Long id);

    @Nullable PetDTO findById(Long id);

    Page<PetDTO> findAllByOwnerId(Long ownerId, Pageable pageable);
}