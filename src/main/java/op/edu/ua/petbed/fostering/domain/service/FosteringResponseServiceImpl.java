package op.edu.ua.petbed.fostering.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.fostering.FosteringResponseService;
import op.edu.ua.petbed.fostering.domain.model.FosteringPost;
import op.edu.ua.petbed.fostering.domain.model.FosteringResponse;
import op.edu.ua.petbed.fostering.domain.repository.FosteringPostRepository;
import op.edu.ua.petbed.fostering.domain.repository.FosteringResponseRepository;
import op.edu.ua.petbed.fostering.domain.repository.FosteringSavedPostRepository;
import op.edu.ua.petbed.fostering.domain.repository.FosteringViewHistoryRepository;
import op.edu.ua.petbed.common.dto.FosteringResponseDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.user.UserService;
import op.edu.ua.petbed.common.dto.UserDTO;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class FosteringResponseServiceImpl implements FosteringResponseService {

    private final FosteringResponseRepository fosteringResponseRepository;
    private final FosteringPostRepository fosteringPostRepository;
    private final FosteringSavedPostRepository fosteringSavedPostRepository;
    private final FosteringViewHistoryRepository fosteringViewHistoryRepository;
    private final PetService petService;
    private final UserService userService;

    @Override
    @Transactional
    public FosteringResponseDTO create(Long postId, Long responderId, String comment) {
        FosteringPost post = fosteringPostRepository.findById(postId)
                .orElseThrow(() -> new PetBedException("Fostering post not found with id: " + postId,
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        if (!post.isActive()) {
            throw new PetBedException("Cannot respond to non-active fostering post",
                    PetBedException.ErrorCode.ADOPTION_POST_INVALID_STATUS);
        }

        if (hasResponded(postId, responderId)) {
            throw new PetBedException("You have already responded to this post",
                    PetBedException.ErrorCode.ADOPTION_RESPONSE_ALREADY_EXISTS);
        }

        PetDTO pet = petService.findById(post.getPetId());
        FosteringResponse response = FosteringResponse.create(postId, responderId, comment);
        FosteringResponse saved = fosteringResponseRepository.save(response);

        log.info("Created fostering response: id={}, postId={}, responderId={}",
                saved.getIdOrThrow(), postId, responderId);

        return mapToResponseDTO(saved, pet.name());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FosteringResponseDTO> findByPostId(Long postId, Pageable pageable) {
        FosteringPost post = fosteringPostRepository.findById(postId)
                .orElseThrow(() -> new PetBedException("Fostering post not found with id: " + postId,
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());

        return fosteringResponseRepository.findByPostIdOrderByStatusPriority(postId, pageable)
                .map(r -> mapToResponseDTO(r, pet.name()));
    }

    @Override
    @Transactional(readOnly = true)
    public FosteringResponseDTO findById(Long id) {
        FosteringResponse response = fosteringResponseRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Fostering response not found with id: " + id,
                        PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        FosteringPost post = fosteringPostRepository.findById(response.getFosteringPostId())
                .orElseThrow(() -> new PetBedException("Fostering post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());
        return mapToResponseDTO(response, pet.name());
    }

    @Override
    @Transactional
    public void confirmByOwner(Long responseId, Long ownerId) {
        FosteringResponse response = fosteringResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Fostering response not found with id: " + responseId,
                        PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        FosteringPost post = fosteringPostRepository.findById(response.getFosteringPostId())
                .orElseThrow(() -> new PetBedException("Fostering post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());
        if (!pet.ownerId().equals(ownerId)) {
            throw new PetBedException("Not authorized to confirm this response",
                    PetBedException.ErrorCode.ADOPTION_POST_NOT_AUTHORIZED);
        }

        response.confirmByOwner();
        fosteringResponseRepository.save(response);

        post.confirmOwner(responseId);
        fosteringPostRepository.save(post);

        fosteringResponseRepository.rejectOthers(post.getIdOrThrow(), responseId);

        log.info("Owner confirmed fostering response: responseId={}, postId={}", responseId, post.getIdOrThrow());
    }

    @Override
    @Transactional
    public void rejectByOwner(Long responseId, Long ownerId) {
        FosteringResponse response = fosteringResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Fostering response not found with id: " + responseId,
                        PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        FosteringPost post = fosteringPostRepository.findById(response.getFosteringPostId())
                .orElseThrow(() -> new PetBedException("Fostering post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());
        if (!pet.ownerId().equals(ownerId)) {
            throw new PetBedException("Not authorized to reject this response",
                    PetBedException.ErrorCode.ADOPTION_POST_NOT_AUTHORIZED);
        }

        response.rejectByOwner();
        fosteringResponseRepository.save(response);

        log.info("Owner rejected fostering response: responseId={}, postId={}", responseId, post.getIdOrThrow());
    }

    @Override
    @Transactional
    public void restore(Long responseId, Long ownerId) {
        FosteringResponse response = fosteringResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Fostering response not found with id: " + responseId,
                        PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        FosteringPost post = fosteringPostRepository.findById(response.getFosteringPostId())
                .orElseThrow(() -> new PetBedException("Fostering post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());
        if (!pet.ownerId().equals(ownerId)) {
            throw new PetBedException("Not authorized to restore this response",
                    PetBedException.ErrorCode.ADOPTION_POST_NOT_AUTHORIZED);
        }

        response.restore();
        fosteringResponseRepository.save(response);

        log.info("Owner restored fostering response: responseId={}, postId={}", responseId, post.getIdOrThrow());
    }

    @Override
    @Transactional
    public void finalConfirm(Long responseId, Long responderId) {
        FosteringResponse response = fosteringResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Fostering response not found with id: " + responseId,
                        PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        if (!response.getResponderId().equals(responderId)) {
            throw new PetBedException("Not authorized to finalize this response",
                    PetBedException.ErrorCode.ADOPTION_POST_NOT_AUTHORIZED);
        }

        response.finalConfirm();
        fosteringResponseRepository.save(response);

        FosteringPost post = fosteringPostRepository.findById(response.getFosteringPostId())
                .orElseThrow(() -> new PetBedException("Fostering post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        post.complete(responderId);
        fosteringPostRepository.save(post);

        fosteringSavedPostRepository.deleteByPostId(post.getIdOrThrow());
        fosteringViewHistoryRepository.deleteByPostId(post.getIdOrThrow());

        petService.updateStatus(post.getPetId(), PetStatus.FOSTERED);

        log.info("Fosterer finalized fostering: responseId={}, postId={}, tempOwnerId={}",
                responseId, post.getIdOrThrow(), responderId);
    }

    @Override
    @Transactional
    public void declineFinalization(Long responseId, Long responderId) {
        FosteringResponse response = fosteringResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Fostering response not found with id: " + responseId,
                        PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        if (!response.getResponderId().equals(responderId)) {
            throw new PetBedException("Not authorized to decline this response",
                    PetBedException.ErrorCode.ADOPTION_POST_NOT_AUTHORIZED);
        }

        response.cancel();
        fosteringResponseRepository.save(response);

        FosteringPost post = fosteringPostRepository.findById(response.getFosteringPostId())
                .orElseThrow(() -> new PetBedException("Fostering post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        post.resetToActive();
        fosteringPostRepository.save(post);

        log.info("Fosterer declined fostering: responseId={}, postId={}", responseId, post.getIdOrThrow());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasResponded(Long postId, Long userId) {
        return fosteringResponseRepository.existsByFosteringPostIdAndResponderId(postId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveResponse(Long postId, Long userId) {
        return fosteringResponseRepository.existsActiveByFosteringPostIdAndResponderId(postId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FosteringResponseDTO> findByResponderId(Long responderId, Pageable pageable) {
        return fosteringResponseRepository.findByResponderId(responderId, pageable)
                .map(this::mapToResponseDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByResponderId(Long responderId) {
        return fosteringResponseRepository.existsByResponderId(responderId);
    }

    private FosteringResponseDTO mapToResponseDTO(FosteringResponse response, String postPetName) {
        UserDTO responder = userService.findById(response.getResponderId());
        return FosteringResponseDTO.fromEntity(
                response,
                responder.telegramUsername(),
                responder.telegramUsername(),
                postPetName
        );
    }

    private FosteringResponseDTO mapToResponseDTO(FosteringResponse response) {
        FosteringPost post = fosteringPostRepository.findById(response.getFosteringPostId())
                .orElseThrow(() -> new PetBedException("Fostering post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));
        PetDTO pet = petService.findById(post.getPetId());
        return mapToResponseDTO(response, pet.name());
    }
}
