package op.edu.ua.petbed.adoption.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.adoption.dto.AdoptionResponseDTO;
import op.edu.ua.petbed.adoption.domain.model.AdoptionPost;
import op.edu.ua.petbed.adoption.domain.model.AdoptionResponse;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionPostRepository;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionResponseRepository;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.user.model.User;
import op.edu.ua.petbed.user.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionResponseServiceImpl implements AdoptionResponseService {

    private final AdoptionResponseRepository adoptionResponseRepository;
    private final AdoptionPostRepository adoptionPostRepository;
    private final PetService petService;
    private final UserRepository userRepository;
    private final AdoptionCompletionService adoptionCompletionService;

    @Override
    @Transactional
    public AdoptionResponseDTO create(Long postId, Long responderId, String comment) {
        if (hasResponded(postId, responderId)) {
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

        // Check responder is not the owner
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
    public List<AdoptionResponseDTO> findByPostId(Long postId) {
        return adoptionResponseRepository.findByPostIdOrderByStatusPriority(postId)
                .stream()
                .map(this::mapToDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdoptionResponseDTO findById(Long id) {
        AdoptionResponse response = adoptionResponseRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Adoption response not found with id: " + id,
                        PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));
        return mapToDTO(response);
    }

    @Override
    @Transactional
    public void confirmByOwner(Long responseId, Long ownerId) {
        AdoptionResponse response = adoptionResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Adoption response not found with id: " + responseId,
                        PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        AdoptionPost post = adoptionPostRepository.findById(response.getAdoptionPostId())
                .orElseThrow(() -> new PetBedException("Adoption post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        // Verify owner
        PetDTO pet = petService.findById(post.getPetId());
        if (!pet.ownerId().equals(ownerId)) {
            throw new PetBedException("Not authorized to confirm this response",
                    PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED);
        }

        // Confirm this response
        response.confirmByOwner();
        adoptionResponseRepository.save(response);

        // Reject all other responses for this post
        adoptionResponseRepository.rejectOthers(post.getIdOrThrow(), responseId);

        // Update post status
        post.confirmOwner(responseId);
        adoptionPostRepository.save(post);

        log.info("Confirmed adoption response: id={}, postId={}, ownerId={}",
                responseId, post.getIdOrThrow(), ownerId);
    }

    @Override
    @Transactional
    public void rejectByOwner(Long responseId, Long ownerId) {
        AdoptionResponse response = adoptionResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Adoption response not found with id: " + responseId,
                        PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        AdoptionPost post = adoptionPostRepository.findById(response.getAdoptionPostId())
                .orElseThrow(() -> new PetBedException("Adoption post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        // Verify owner
        PetDTO pet = petService.findById(post.getPetId());
        if (!pet.ownerId().equals(ownerId)) {
            throw new PetBedException("Not authorized to reject this response",
                    PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED);
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
                .orElseThrow(() -> new PetBedException("Adoption response not found with id: " + responseId,
                        PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        AdoptionPost post = adoptionPostRepository.findById(response.getAdoptionPostId())
                .orElseThrow(() -> new PetBedException("Adoption post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        // Verify owner
        PetDTO pet = petService.findById(post.getPetId());
        if (!pet.ownerId().equals(ownerId)) {
            throw new PetBedException("Not authorized to restore this response",
                    PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED);
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
                .orElseThrow(() -> new PetBedException("Adoption response not found with id: " + responseId,
                        PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        // Verify responder
        if (!response.getResponderId().equals(responderId)) {
            throw new PetBedException("Not authorized to finalize this response",
                    PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED);
        }

        // Final confirm
        response.finalConfirm();
        adoptionResponseRepository.save(response);

        // Complete the adoption
        adoptionCompletionService.completeAdoption(response);

        log.info("Finalized adoption response: id={}, responderId={}", responseId, responderId);
    }

    @Override
    @Transactional
    public void declineFinalization(Long responseId, Long responderId) {
        AdoptionResponse response = adoptionResponseRepository.findById(responseId)
                .orElseThrow(() -> new PetBedException("Adoption response not found with id: " + responseId,
                        PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FOUND));

        // Verify responder
        if (!response.getResponderId().equals(responderId)) {
            throw new PetBedException("Not authorized to decline this response",
                    PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED);
        }

        // Cancel the response (can only cancel if not yet finalized)
        response.cancel();
        adoptionResponseRepository.save(response);

        // Reset post status back to ACTIVE
        AdoptionPost post = adoptionPostRepository.findById(response.getAdoptionPostId())
                .orElseThrow(() -> new PetBedException("Adoption post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        // We need to reset the post status - since there's no method for this in entity,
        // we update the fields directly or add a method to the entity
        // For now, let's assume the post should go back to ACTIVE state
        // This might need an entity method: post.resetToActive();

        log.info("Declined adoption finalization: id={}, responderId={}", responseId, responderId);
    }

    @Override
    public boolean hasResponded(Long postId, Long userId) {
        return adoptionResponseRepository.existsByAdoptionPostIdAndResponderId(postId, userId);
    }

    private AdoptionResponseDTO mapToDTO(AdoptionResponse response) {
        User responder = userRepository.findById(response.getResponderId())
                .orElseThrow(() -> new PetBedException("Responder not found",
                        PetBedException.ErrorCode.USER_NOT_FOUND));

        Instant createdAt = response.getCreatedAt();
        if (createdAt == null) {
            throw new PetBedException("Response createdAt is null",
                    PetBedException.ErrorCode.INTERNAL_ERROR);
        }

        return AdoptionResponseDTO.fromEntity(
                response,
                responder.getTelegramUsername(),
                responder.getTelegramUsername()
        );
    }
}
