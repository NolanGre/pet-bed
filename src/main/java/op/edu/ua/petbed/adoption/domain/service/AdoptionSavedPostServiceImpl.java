package op.edu.ua.petbed.adoption.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.adoption.AdoptionSavedPostService;
import op.edu.ua.petbed.adoption.dto.AdoptionPostDTO;
import op.edu.ua.petbed.adoption.domain.model.AdoptionPost;
import op.edu.ua.petbed.adoption.domain.model.AdoptionSavedPost;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionPostRepository;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionSavedPostRepository;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.pet.PetService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionSavedPostServiceImpl implements AdoptionSavedPostService {

    private final AdoptionSavedPostRepository adoptionSavedPostRepository;
    private final AdoptionPostRepository adoptionPostRepository;
    private final PetService petService;

    @Override
    @Transactional
    public void save(Long postId, Long userId) {
        if (isSaved(postId, userId)) {
            return; // Already saved, idempotent
        }

        // Verify post exists
        AdoptionPost post = adoptionPostRepository.findById(postId)
                .orElseThrow(() -> new PetBedException("Adoption post not found with id: " + postId,
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        AdoptionSavedPost savedPost = AdoptionSavedPost.create(postId, userId);
        adoptionSavedPostRepository.save(savedPost);

        log.info("Saved adoption post: postId={}, userId={}", postId, userId);
    }

    @Override
    @Transactional
    public void unsave(Long postId, Long userId) {
        if (!isSaved(postId, userId)) {
            return; // Not saved, idempotent
        }

        adoptionSavedPostRepository.deleteByPostIdAndUserId(postId, userId);

        log.info("Unsaved adoption post: postId={}, userId={}", postId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AdoptionPostDTO> findSavedByUserId(Long userId) {
        return adoptionSavedPostRepository.findByUserId(userId)
                .stream()
                .map(savedPost -> {
                    AdoptionPost post = adoptionPostRepository.findById(savedPost.getPostId())
                            .orElseThrow(() -> new PetBedException("Adoption post not found",
                                    PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));
                    return mapToDTO(post);
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSaved(Long postId, Long userId) {
        return adoptionSavedPostRepository.existsByPostIdAndUserId(postId, userId);
    }

    private AdoptionPostDTO mapToDTO(AdoptionPost post) {
        PetDTO pet = petService.findById(post.getPetId());

        Instant createdAt = post.getCreatedAt();
        if (createdAt == null) {
            throw new PetBedException("Post createdAt is null",
                    PetBedException.ErrorCode.INTERNAL_ERROR);
        }

        return AdoptionPostDTO.fromEntity(post, pet.name(), pet.photoId(), pet.breed(), pet.color());
    }
}
