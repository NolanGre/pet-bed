package op.edu.ua.petbed.adoption.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.adoption.domain.model.AdoptionPost;
import op.edu.ua.petbed.adoption.domain.model.AdoptionResponse;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionPostRepository;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionResponseRepository;
import op.edu.ua.petbed.common.dto.AdoptionResponseDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionResponseServiceImpl implements AdoptionResponseService {

    private final AdoptionResponseRepository adoptionResponseRepository;
    private final AdoptionPostRepository adoptionPostRepository;
    private final PetService petService;
    private final UserService userService;
    private final AdoptionCompletionService adoptionCompletionService;

    @Override
    @Transactional
    public AdoptionResponseDTO create(Long postId, Long responderId, String comment) {
        var existingResponse = adoptionResponseRepository.findByAdoptionPostIdAndResponderId(postId, responderId);
        if (existingResponse.isPresent()) {
            AdoptionResponse existing = existingResponse.get();
            if (existing.getStatus() == op.edu.ua.petbed.adoption.domain.model.AdoptionResponseStatus.CANCELLED) {
                log.info("Reactivating cancelled adoption response: id={}, postId={}, responderId={}",
                        existing.getIdOrThrow(), postId, responderId);
                existing.reactivate(comment);
                return mapToDTO(adoptionResponseRepository.save(existing));
            }
            throw new PetBedException("User has already responded to this post",
                    PetBedException.ErrorCode.ADOPTION_RESPONSE_ALREADY_EXISTS);
        }

        AdoptionPost post = adoptionPostRepository.findById(postId)
                .orElseThrow(() -> new PetBedException("Adoption post not found with id: " + postId,
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        if (!post.isActive()) {
            throw new PetBedException("Cannot respond to non-active post",
                    PetBedException.ErrorCode.ADOPTION_POST_INVALID_STATUS);
        }

        PetDTO pet = petService.findById(post.getPetId());
        if (pet.ownerId().equals(responderId)) {
            throw new PetBedException("Cannot respond to your own post",
                    PetBedException.ErrorCode.ADOPTION_RESPONSE_SELF_RESPONSE);
        }

        AdoptionResponse response = AdoptionResponse.create(postId, responderId, comment);
        AdoptionResponse saved = adoptionResponseRepository.save(response);

        log.info("Created adoption response: id={}, postId={}, responderId={}",
                saved.getIdOrThrow(), postId, responderId);

        return mapToDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdoptionResponseDTO> findByPostId(Long postId, Pageable pageable) {
        return adoptionResponseRepository.findByPostIdOrderByStatusPriority(postId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public AdoptionResponseDTO findById(Long id) {
        AdoptionResponse response = adoptionResponseRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Adoption response not found with id: " + id, PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));
        return mapToDTO(response);
    }

    @Override
    @Transactional
    public void confirmByOwner(Long responseId, Long ownerId) {
        AdoptionResponse response = adoptionResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Adoption response not found with id: " + responseId, PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        AdoptionPost post = adoptionPostRepository.findById(response.getAdoptionPostId())
                .orElseThrow(() -> new PetBedException("Adoption post not found", PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());
        if (!pet.ownerId().equals(ownerId)) {
            throw new PetBedException("Not authorized to confirm this response", PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED);
        }

        response.confirmByOwner();
        adoptionResponseRepository.save(response);

        adoptionResponseRepository.rejectOthers(post.getIdOrThrow(), responseId);

        post.confirmOwner(responseId);
        adoptionPostRepository.save(post);

        log.info("Confirmed adoption response: id={}, postId={}, ownerId={}",
                responseId, post.getIdOrThrow(), ownerId);
    }

    @Override
    @Transactional
    public void rejectByOwner(Long responseId, Long ownerId) {
        AdoptionResponse response = adoptionResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Adoption response not found with id: " + responseId, PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        AdoptionPost post = adoptionPostRepository.findById(response.getAdoptionPostId())
                .orElseThrow(() -> new PetBedException("Adoption post not found", PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());
        if (!pet.ownerId().equals(ownerId)) {
            throw new PetBedException("Not authorized to reject this response", PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED);
        }

        response.rejectByOwner();
        adoptionResponseRepository.save(response);

        log.info("Rejected adoption response: id={}, postId={}, ownerId={}",
                responseId, post.getIdOrThrow(), ownerId);
    }

    @Override
    @Transactional
    public void restore(Long responseId, Long ownerId) {
        AdoptionResponse response = adoptionResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Adoption response not found with id: " + responseId, PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        AdoptionPost post = adoptionPostRepository.findById(response.getAdoptionPostId())
                .orElseThrow(() -> new PetBedException("Adoption post not found", PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());
        if (!pet.ownerId().equals(ownerId)) {
            throw new PetBedException("Not authorized to restore this response", PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED);
        }

        response.restore();
        adoptionResponseRepository.save(response);

        log.info("Restored adoption response: id={}, postId={}, ownerId={}",
                responseId, post.getIdOrThrow(), ownerId);
    }

    @Override
    @Transactional
    public void finalConfirm(Long responseId, Long responderId) {
        AdoptionResponse response = adoptionResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Adoption response not found with id: " + responseId, PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        if (!response.getResponderId().equals(responderId)) {
            throw new PetBedException("Not authorized to finalize this response", PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED);
        }

        if (response.getStatus() != op.edu.ua.petbed.adoption.domain.model.AdoptionResponseStatus.CONFIRMED_BY_OWNER) {
            throw new PetBedException("Response must be confirmed by owner first", PetBedException.ErrorCode.ADOPTION_RESPONSE_INVALID_STATUS);
        }

        response.finalConfirm();
        adoptionResponseRepository.save(response);

        AdoptionPost post = adoptionPostRepository.findById(response.getAdoptionPostId())
                .orElseThrow(() -> new PetBedException("Adoption post not found", PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        post.complete();
        adoptionPostRepository.save(post);

        adoptionCompletionService.completeAdoption(response);

        log.info("Finalized adoption response: id={}, responderId={}", responseId, responderId);
    }

    @Override
    @Transactional
    public void declineFinalization(Long responseId, Long responderId) {
        AdoptionResponse response = adoptionResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Adoption response not found with id: " + responseId, PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        if (!response.getResponderId().equals(responderId)) {
            throw new PetBedException("Not authorized to decline this response", PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED);
        }

        response.cancel();
        adoptionResponseRepository.save(response);

        AdoptionPost post = adoptionPostRepository.findById(response.getAdoptionPostId())
                .orElseThrow(() -> new PetBedException("Adoption post not found", PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        post.resetToActive();
        adoptionPostRepository.save(post);

        log.info("Declined adoption finalization: id={}, responderId={}", responseId, responderId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasResponded(Long postId, Long userId) {
        return adoptionResponseRepository.existsByAdoptionPostIdAndResponderId(postId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasActiveResponse(Long postId, Long userId) {
        return adoptionResponseRepository.existsActiveByAdoptionPostIdAndResponderId(postId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdoptionResponseDTO> findByResponderId(Long responderId, Pageable pageable) {
        return adoptionResponseRepository.findByResponderId(responderId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByResponderId(Long responderId) {
        return adoptionResponseRepository.existsByResponderId(responderId);
    }

    private AdoptionResponseDTO mapToDTO(AdoptionResponse response) {
        var responder = userService.findById(response.getResponderId());

        var post = adoptionPostRepository.findById(response.getAdoptionPostId())
                .orElseThrow(() -> new PetBedException("Adoption post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));
        var pet = petService.findById(post.getPetId());

        Instant createdAt = response.getCreatedAt();
        if (createdAt == null) {
            throw new PetBedException("Response createdAt is null", PetBedException.ErrorCode.INTERNAL_ERROR);
        }

        return AdoptionResponseDTO.fromEntity(
                response,
                responder.telegramUsername(),
                responder.telegramUsername(),
                pet.name()
        );
    }
}
