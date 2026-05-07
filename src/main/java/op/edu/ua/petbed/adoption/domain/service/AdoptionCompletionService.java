package op.edu.ua.petbed.adoption.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.adoption.domain.model.AdoptionPost;
import op.edu.ua.petbed.adoption.domain.model.AdoptionResponse;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionPostRepository;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.pet.PetService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Internal service responsible for completing the adoption process.
 * Transfers pet ownership when both parties have confirmed.
 */
@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionCompletionService {

    private final AdoptionPostRepository adoptionPostRepository;
    private final PetService petService;

    /**
     * Completes the adoption process by transferring the pet to the new owner.
     *
     * @param response the finalized adoption response
     */
    @Transactional
    public void completeAdoption(AdoptionResponse response) {
        if (!response.isFinalized()) {
            throw new PetBedException("Response must be finalized before completing adoption",
                    PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_FINALIZED);
        }

        AdoptionPost post = adoptionPostRepository.findById(response.getAdoptionPostId())
                .orElseThrow(() -> new PetBedException("Adoption post not found",
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        // Complete the post
        post.complete();
        adoptionPostRepository.save(post);

        // Transfer pet ownership
        petService.transferOwnership(post.getPetId(), response.getResponderId());

        // Reset pet status to DEFAULT
        petService.updateStatus(post.getPetId(), PetStatus.DEFAULT);

        log.info("Completed adoption: postId={}, petId={}, newOwnerId={}",
                post.getIdOrThrow(), post.getPetId(), response.getResponderId());
    }
}
