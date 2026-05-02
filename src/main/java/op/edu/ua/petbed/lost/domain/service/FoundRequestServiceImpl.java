package op.edu.ua.petbed.lost.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.FoundRequestDTO;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.lost.FoundRequestService;
import op.edu.ua.petbed.lost.application.dto.CreateFoundRequestDTO;
import op.edu.ua.petbed.lost.application.event.FoundRequestCreatedEvent;
import op.edu.ua.petbed.lost.application.matching.TextNormalizer;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import op.edu.ua.petbed.lost.domain.repository.FoundRequestRepository;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
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
    private final TextNormalizer textNormalizer;

    @Override
    @Transactional
    public FoundRequestDTO create(CreateFoundRequestDTO dto) {
        UserDTO finder = userService.findById(dto.finderId());

        CreateFoundRequestDTO normalizedDto = CreateFoundRequestDTO.builder()
                .finderId(dto.finderId())
                .photoUrl(dto.photoUrl())
                .petType(dto.petType())
                .location(dto.location())
                .breed(normalize(dto.breed()))
                .color(normalize(dto.color()))
                .coat(normalize(dto.coat()))
                .size(normalizeSize(dto.size()))
                .sex(normalizeSex(dto.sex()))
                .features(normalize(dto.features()))
                .build();

        FoundRequest foundRequest = FoundRequest.create(normalizedDto);
        FoundRequest saved = foundRequestRepository.save(foundRequest);
        foundRequestRepository.flush();

        log.info("Created found request: id={}, finderId={}, petType={}",
                saved.getIdOrThrow(), dto.finderId(), dto.petType());

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
                saved.getBreedText(),
                saved.getColorText(),
                saved.getCoatText(),
                saved.getSizeText(),
                saved.getSexText(),
                saved.getFeaturesText(),
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
                foundRequest.getBreedText(),
                foundRequest.getColorText(),
                foundRequest.getCoatText(),
                foundRequest.getSizeText(),
                foundRequest.getSexText(),
                foundRequest.getFeaturesText(),
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
                            fr.getBreedText(),
                            fr.getColorText(),
                            fr.getCoatText(),
                            fr.getSizeText(),
                            fr.getSexText(),
                            fr.getFeaturesText(),
                            ca
                    );
                })
                .toList();
    }

    private @Nullable String normalize(@Nullable String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return textNormalizer.normalize(text);
    }

    private @Nullable String normalizeSize(@Nullable String size) {
        if (size == null || size.isBlank()) {
            return null;
        }
        return switch (size.toUpperCase()) {
            case "SMALL" -> "малий";
            case "MEDIUM" -> "середній";
            case "LARGE" -> "великий";
            default -> size.trim().toLowerCase();
        };
    }

    private @Nullable String normalizeSex(@Nullable String sex) {
        if (sex == null || sex.isBlank()) {
            return null;
        }
        return switch (sex.toUpperCase()) {
            case "MALE" -> "він";
            case "FEMALE" -> "вона";
            default -> sex.trim().toLowerCase();
        };
    }
}
