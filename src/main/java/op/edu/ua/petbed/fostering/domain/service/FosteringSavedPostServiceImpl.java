package op.edu.ua.petbed.fostering.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.fostering.FosteringSavedPostService;
import op.edu.ua.petbed.fostering.domain.model.FosteringPost;
import op.edu.ua.petbed.fostering.domain.model.FosteringSavedPost;
import op.edu.ua.petbed.fostering.domain.repository.FosteringPostRepository;
import op.edu.ua.petbed.fostering.domain.repository.FosteringSavedPostRepository;
import op.edu.ua.petbed.common.dto.FosteringPostDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.pet.PetService;
import org.jspecify.annotations.NullMarked;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class FosteringSavedPostServiceImpl implements FosteringSavedPostService {

    private final FosteringSavedPostRepository fosteringSavedPostRepository;
    private final FosteringPostRepository fosteringPostRepository;
    private final PetService petService;

    @Override
    @Transactional
    public void save(Long postId, Long userId) {
        if (isSaved(postId, userId)) {
            return;
        }

        FosteringPost post = fosteringPostRepository.findById(postId)
                .orElseThrow(() -> new PetBedException("Fostering post not found with id: " + postId,
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        FosteringSavedPost saved = FosteringSavedPost.create(postId, userId);
        fosteringSavedPostRepository.save(saved);

        log.info("User {} saved fostering post {}", userId, postId);
    }

    @Override
    @Transactional
    public void unsave(Long postId, Long userId) {
        if (!isSaved(postId, userId)) {
            return;
        }

        fosteringSavedPostRepository.deleteById(new op.edu.ua.petbed.fostering.domain.model.FosteringSavedPostId(postId, userId));

        log.info("User {} unsaved fostering post {}", userId, postId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FosteringPostDTO> findSavedByUserId(Long userId, Pageable pageable) {
        return fosteringSavedPostRepository.findByUserId(userId, pageable)
                .map(this::mapToPostDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSaved(Long postId, Long userId) {
        return fosteringSavedPostRepository.existsByPostIdAndUserId(postId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByUserId(Long userId) {
        return fosteringSavedPostRepository.existsByUserId(userId);
    }

    private FosteringPostDTO mapToPostDTO(FosteringSavedPost savedPost) {
        FosteringPost post = fosteringPostRepository.findById(savedPost.getPostId())
                .orElseThrow(() -> new PetBedException("Fostering post not found with id: " + savedPost.getPostId(),
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());
        return FosteringPostDTO.fromEntity(post, pet.name(), pet.photoId(), pet.breed(), pet.color(),
                pet.age(), pet.sex(), pet.size(), pet.specialMarks());
    }
}
