package op.edu.ua.petbed.lost.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.FoundRequestDTO;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.FoundRequestService;
import op.edu.ua.petbed.lost.application.event.FoundRequestCreatedEvent;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import op.edu.ua.petbed.lost.domain.repository.FoundRequestRepository;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class FoundRequestServiceImpl implements FoundRequestService {

    private final FoundRequestRepository foundRequestRepository;
    private final UserService userService;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public FoundRequestDTO create(Long finderId, String photoUrl, PetType petType, Point location, String description) {
        UserDTO finder = userService.findById(finderId);

        FoundRequest foundRequest = FoundRequest.create(finder.id(), photoUrl, petType, location, description);
        FoundRequest saved = foundRequestRepository.save(foundRequest);
        foundRequestRepository.flush();

        log.info("Created found request: id={}, finderId={}, petType={}", saved.getIdOrThrow(), finderId, petType);

        eventPublisher.publishEvent(new FoundRequestCreatedEvent(saved.getIdOrThrow()));

        Instant createdAt = saved.getCreatedAt();
        if (createdAt == null) {
            throw new PetBedException("Found request createdAt is null", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
        return new FoundRequestDTO(
                saved.getIdOrThrow(),
                saved.getFinderId(),
                saved.getPhotoUrl(),
                saved.getPetType(),
                saved.getLocation(),
                saved.getDescription(),
                createdAt
        );
    }

    @Override
    @Transactional(readOnly = true)
    public FoundRequestDTO findById(Long id) {
        FoundRequest foundRequest = foundRequestRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Found request not found with id: " + id, PetBedException.ErrorCode.FOUND_REQUEST_REQUIRED));

        Instant createdAt = foundRequest.getCreatedAt();
        if (createdAt == null) {
            throw new PetBedException("Found request createdAt is null", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
        return new FoundRequestDTO(
                foundRequest.getIdOrThrow(),
                foundRequest.getFinderId(),
                foundRequest.getPhotoUrl(),
                foundRequest.getPetType(),
                foundRequest.getLocation(),
                foundRequest.getDescription(),
                createdAt
        );
    }

    @Override
    @Transactional(readOnly = true)
    public List<FoundRequestDTO> findByFinderId(Long finderId) {
        List<FoundRequest> foundRequests = foundRequestRepository.findByFinderId(finderId);

        return foundRequests.stream()
                .map(fr -> {
                    Instant ca = fr.getCreatedAt();
                    if (ca == null) {
                        throw new PetBedException("Found request createdAt is null", PetBedException.ErrorCode.INTERNAL_ERROR);
                    }
                    return new FoundRequestDTO(
                            fr.getIdOrThrow(),
                            fr.getFinderId(),
                            fr.getPhotoUrl(),
                            fr.getPetType(),
                            fr.getLocation(),
                            fr.getDescription(),
                            ca
                    );
                })
                .toList();
    }
}
