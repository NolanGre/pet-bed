package op.edu.ua.petbed.lost.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.LostRequestDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.lost.LostRequestService;
import op.edu.ua.petbed.lost.application.event.LostRequestCreatedEvent;
import op.edu.ua.petbed.lost.application.matching.TextNormalizer;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import op.edu.ua.petbed.lost.domain.repository.LostRequestRepository;
import op.edu.ua.petbed.pet.PetService;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class LostRequestServiceImpl implements LostRequestService {

    private final LostRequestRepository lostRequestRepository;
    private final PetService petService;
    private final ApplicationEventPublisher eventPublisher;
    private final TextNormalizer textNormalizer;

    @Override
    @Transactional
    public LostRequestDTO create(Long petId, String contactInfo, Point location) {
        if (existsByPetId(petId)) {
            throw new PetBedException("Lost request already exists for pet: " + petId,
                    PetBedException.ErrorCode.LOST_REQUEST_INVALID_INPUT);
        }

        PetDTO pet = petService.findById(petId);

        String searchText = LostRequest.generateSearchText(pet);
        String normalizedSearchText = textNormalizer.normalize(searchText);

        LostRequest lostRequest = LostRequest.createWithSearchText(pet, contactInfo, location, normalizedSearchText);
        LostRequest saved = lostRequestRepository.save(lostRequest);
        lostRequestRepository.flush();

        petService.updateStatus(petId, PetStatus.IN_LOST);

        log.info("Created lost request: id={}, petId={}", saved.getIdOrThrow(), petId);

        eventPublisher.publishEvent(new LostRequestCreatedEvent(saved.getIdOrThrow()));

        Instant createdAt = saved.getCreatedAt();
        if (createdAt == null) {
            throw new PetBedException("Lost request createdAt is null", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
        return new LostRequestDTO(
                saved.getIdOrThrow(),
                saved.getPetId(),
                saved.getContactInfo(),
                saved.getLastSeenLocation(),
                saved.getPetType(),
                createdAt
        );
    }

    @Override
    @Transactional(readOnly = true)
    public LostRequestDTO findById(Long id) {
        LostRequest lostRequest = lostRequestRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Lost request not found with id: " + id,
                        PetBedException.ErrorCode.LOST_REQUEST_REQUIRED));
        Instant createdAt = lostRequest.getCreatedAt();
        if (createdAt == null) {
            throw new PetBedException("Lost request createdAt is null", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
        return new LostRequestDTO(
                lostRequest.getIdOrThrow(),
                lostRequest.getPetId(),
                lostRequest.getContactInfo(),
                lostRequest.getLastSeenLocation(),
                lostRequest.getPetType(),
                createdAt
        );
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
                .map(lr -> {
                    Instant ca = lr.getCreatedAt();
                    if (ca == null) {
                        throw new PetBedException("Lost request createdAt is null", PetBedException.ErrorCode.INTERNAL_ERROR);
                    }
                    return new LostRequestDTO(
                            lr.getIdOrThrow(),
                            lr.getPetId(),
                            lr.getContactInfo(),
                            lr.getLastSeenLocation(),
                            lr.getPetType(),
                            ca
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LostRequestDTO> findByOwnerId(Long ownerId, Pageable pageable) {
        return lostRequestRepository.findByOwnerId(ownerId, pageable)
                .map(lr -> {
                    Instant ca = lr.getCreatedAt();
                    if (ca == null) {
                        throw new PetBedException("Lost request createdAt is null", PetBedException.ErrorCode.INTERNAL_ERROR);
                    }
                    return new LostRequestDTO(
                            lr.getIdOrThrow(),
                            lr.getPetId(),
                            lr.getContactInfo(),
                            lr.getLastSeenLocation(),
                            lr.getPetType(),
                            ca
                    );
                });
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
