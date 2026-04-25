package op.edu.ua.petbed.pet.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.CreatePetDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.dto.UpdatePetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.pet.model.Pet;
import op.edu.ua.petbed.pet.repository.PetRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class PetServiceImpl implements PetService {

    private final PetRepository petRepository;

    @Override
    @Transactional
    public PetDTO create(CreatePetDTO dto) {
        Pet pet = Pet.create(dto);
        Pet saved = petRepository.save(pet);
        log.info("Created pet: id={}, ownerId={}, name={}", saved.getIdOrThrow(), saved.getOwnerId(), saved.getName());
        return toDto(saved);
    }

    @Override
    @Transactional
    public PetDTO update(UpdatePetDTO dto) {
        Pet pet = petRepository.findById(dto.id())
                .orElseThrow(() -> new PetBedException("Pet not found with id: " + dto.id(), PetBedException.ErrorCode.PET_NOT_FOUND));

        pet.update(dto);

        Pet saved = petRepository.save(pet);
        log.info("Updated pet: id={}", saved.getIdOrThrow());
        return toDto(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Pet not found with id: " + id, PetBedException.ErrorCode.PET_NOT_FOUND));

        if (!pet.canDelete()) {
            throw new PetBedException("Cannot delete pet with status: " + pet.getStatus(), PetBedException.ErrorCode.PET_CANNOT_DELETE);
        }

        petRepository.delete(pet);
        log.info("Deleted pet: id={}", id);
    }

    @Override
    public PetDTO findById(Long id) {
        return petRepository.findById(id).map(this::toDto)
                .orElseThrow(() -> new PetBedException("Pet not found with id: " + id, PetBedException.ErrorCode.PET_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<PetDTO> findAllByOwnerId(Long ownerId, Pageable pageable) {
        Page<Pet> pets = petRepository.findByOwnerId(ownerId, pageable);
        return pets.map(this::toDto);
    }

    private PetDTO toDto(Pet pet) {
        return new PetDTO(
                pet.getIdOrThrow(),
                pet.getOwnerId(),
                pet.getName(),
                pet.getType(),
                pet.getPhotoId(),
                pet.getBreed(),
                pet.getColor(),
                pet.getColorPattern(),
                pet.getAge(),
                pet.getSex(),
                pet.getSize(),
                pet.getSpecialMarks(),
                pet.getStatus()
        );
    }
}