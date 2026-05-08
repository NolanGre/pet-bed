package op.edu.ua.petbed.adoption.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.common.dto.AdoptionPostDTO;
import op.edu.ua.petbed.common.dto.AdoptionPostDetailDTO;
import op.edu.ua.petbed.common.dto.AdoptionRecommendationDTO;
import op.edu.ua.petbed.common.dto.AdoptionResponseDTO;
import op.edu.ua.petbed.adoption.domain.model.AdoptionPost;
import op.edu.ua.petbed.adoption.domain.model.AdoptionResponse;
import op.edu.ua.petbed.adoption.domain.model.AdoptionViewHistory;
import op.edu.ua.petbed.common.model.AdoptionPostStatus;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionPostRepository;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionResponseRepository;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionSavedPostRepository;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionViewHistoryRepository;
import op.edu.ua.petbed.common.dto.PetDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.user.UserService;
import op.edu.ua.petbed.user.model.User;
import op.edu.ua.petbed.user.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class AdoptionPostServiceImpl implements AdoptionPostService {

    private final AdoptionPostRepository adoptionPostRepository;
    private final AdoptionResponseRepository adoptionResponseRepository;
    private final AdoptionSavedPostRepository adoptionSavedPostRepository;
    private final AdoptionViewHistoryRepository adoptionViewHistoryRepository;
    private final PetService petService;
    private final UserService userService;

    @Override
    @Transactional
    public AdoptionPostDTO create(Long petId, @Nullable String ownerComment) {
        if (existsByPetId(petId)) {
            throw new PetBedException("Pet already has an active adoption post",
                    PetBedException.ErrorCode.ADOPTION_POST_ALREADY_EXISTS);
        }

        PetDTO pet = petService.findById(petId);

        AdoptionPost post = AdoptionPost.create(petId, ownerComment);
        AdoptionPost saved = adoptionPostRepository.save(post);

        petService.updateStatus(petId, PetStatus.IN_ADOPTION);

        log.info("Created adoption post: id={}, petId={}", saved.getIdOrThrow(), petId);

        Instant createdAt = saved.getCreatedAt();
        if (createdAt == null) {
            throw new PetBedException("Adoption post createdAt is null",
                    PetBedException.ErrorCode.INTERNAL_ERROR);
        }

        return AdoptionPostDTO.fromEntity(saved, pet.name(), pet.photoId(), pet.breed(), pet.color());
    }

    @Override
    @Transactional(readOnly = true)
    public AdoptionPostDetailDTO findById(Long id) {
        AdoptionPost post = adoptionPostRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Adoption post not found with id: " + id,
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());

        List<AdoptionResponseDTO> responses = adoptionResponseRepository
                .findByPostIdOrderByStatusPriority(id)
                .stream()
                .map(this::mapToResponseDTO)
                .toList();

        return new AdoptionPostDetailDTO(
                AdoptionPostDTO.fromEntity(post, pet.name(), pet.photoId(), pet.breed(), pet.color()),
                responses
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AdoptionPostDTO> findAllByOwnerId(Long ownerId, Pageable pageable) {
        return adoptionPostRepository.findByOwnerId(ownerId, pageable)
                .map(this::mapToPostDTO);
    }

    @Override
    @Transactional
    public void cancel(Long postId, Long ownerId) {
        AdoptionPost post = adoptionPostRepository.findById(postId)
                .orElseThrow(() -> new PetBedException("Adoption post not found with id: " + postId,
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());
        if (!pet.ownerId().equals(ownerId)) {
            throw new PetBedException("Not authorized to cancel this post",
                    PetBedException.ErrorCode.ADOPTION_POST_NOT_AUTHORIZED);
        }

        post.cancel();
        adoptionPostRepository.save(post);

        petService.updateStatus(post.getPetId(), PetStatus.DEFAULT);

        log.info("Cancelled adoption post: id={}, petId={}", postId, post.getPetId());
    }

    @Override
    public boolean existsByPetId(Long petId) {
        return adoptionPostRepository.existsByPetId(petId);
    }

    @Override
    @Transactional(readOnly = true)
    public @Nullable AdoptionRecommendationDTO findNextForFeed(Long userId, int offset) {
        Pageable pageable = PageRequest.of(offset, 1);
        var posts = adoptionPostRepository.findUnviewedActivePosts(userId, pageable);

        if (posts.isEmpty()) {
            return null;
        }

        AdoptionPost post = posts.getContent().get(0);
        PetDTO pet = petService.findById(post.getPetId());
        boolean isSaved = adoptionSavedPostRepository.existsByPostIdAndUserId(post.getIdOrThrow(), userId);

        return new AdoptionRecommendationDTO(
                post.getIdOrThrow(),
                pet.id(),
                pet.name(),
                pet.photoId(),
                pet.breed(),
                pet.color(),
                post.getOwnerComment(),
                post.getCreatedAt(),
                isSaved
        );
    }

    @Override
    @Transactional
    public void recordView(Long postId, Long userId) {
        if (!adoptionViewHistoryRepository.existsByPostIdAndUserId(postId, userId)) {
            AdoptionViewHistory viewHistory = AdoptionViewHistory.create(postId, userId);
            adoptionViewHistoryRepository.save(viewHistory);
        }

        userService.incrementAdoptionHistoryOffset(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public int getOffset(Long userId) {
        return userService.getAdoptionHistoryOffset(userId);
    }

    @Override
    @Transactional
    public void resetOffset(Long userId) {
        userService.resetAdoptionHistoryOffset(userId);
        adoptionViewHistoryRepository.deleteByUserId(userId);
    }

    private AdoptionPostDTO mapToPostDTO(AdoptionPost post) {
        PetDTO pet = petService.findById(post.getPetId());
        return AdoptionPostDTO.fromEntity(post, pet.name(), pet.photoId(), pet.breed(), pet.color());
    }

    private AdoptionResponseDTO mapToResponseDTO(AdoptionResponse response) {
        var responder = userService.findById(response.getResponderId());

        return AdoptionResponseDTO.fromEntity(
                response,
                responder.telegramUsername(),
                responder.telegramUsername()
        );
    }
}
