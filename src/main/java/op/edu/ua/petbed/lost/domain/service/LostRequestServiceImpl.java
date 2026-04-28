package op.edu.ua.petbed.lost.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.lost.application.dto.LostRequestDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.lost.LostRequestService;
import op.edu.ua.petbed.lost.application.event.LostRequestCreatedEvent;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import op.edu.ua.petbed.lost.domain.repository.LostRequestRepository;
import op.edu.ua.petbed.pet.PetService;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class LostRequestServiceImpl implements LostRequestService {

    private final LostRequestRepository lostRequestRepository;
    private final PetService petService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public LostRequestDTO create(Long petId, String contactInfo, Point location) {
        if (existsByPetId(petId)) {
            throw new PetBedException("Lost request already exists for pet: " + petId,
                    PetBedException.ErrorCode.LOST_REQUEST_INVALID_INPUT);
        }

        PetDTO pet = petService.findById(petId);

        LostRequest lostRequest = LostRequest.create(pet, contactInfo, location);
        LostRequest saved = lostRequestRepository.save(lostRequest);

        petService.updateStatus(petId, PetStatus.IN_LOST);

        log.info("Created lost request: id={}, petId={}", saved.getIdOrThrow(), petId);

        eventPublisher.publishEvent(new LostRequestCreatedEvent(saved.getIdOrThrow()));

        return LostRequestDTO.fromEntity(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public LostRequestDTO findById(Long id) {
        LostRequest lostRequest = lostRequestRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Lost request not found with id: " + id,
                        PetBedException.ErrorCode.LOST_REQUEST_REQUIRED));
        return LostRequestDTO.fromEntity(lostRequest);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LostRequestDTO> findActiveByOwnerId(Long ownerId) {
        return petService.findAllByOwnerId(ownerId, org.springframework.data.domain.Pageable.unpaged())
                .getContent()
                .stream()
                .map(PetDTO::id)
                .map(lostRequestRepository::findByPetId)
                .filter(java.util.Objects::nonNull)
                .map(LostRequestDTO::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public void cancel(Long lostRequestId) {
        LostRequest lostRequest = lostRequestRepository.findById(lostRequestId)
                .orElseThrow(() -> new PetBedException("Lost request not found with id: " + lostRequestId,
                        PetBedException.ErrorCode.LOST_REQUEST_REQUIRED));

        petService.updateStatus(lostRequest.getPetId(), PetStatus.DEFAULT);
        lostRequestRepository.deleteByPetId(lostRequest.getPetId());
        log.info("Cancelled lost request: id={}, petId={}", lostRequestId, lostRequest.getPetId());
    }

    @Override
    public boolean existsByPetId(Long petId) {
        return lostRequestRepository.existsByPetId(petId);
    }
}
