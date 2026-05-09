package op.edu.ua.petbed.adoption.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.adoption.AdoptionPostService;
import op.edu.ua.petbed.adoption.domain.model.AdoptionPost;
import op.edu.ua.petbed.adoption.domain.model.AdoptionResponse;
import op.edu.ua.petbed.adoption.domain.model.AdoptionViewHistory;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionPostRepository;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionResponseRepository;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionSavedPostRepository;
import op.edu.ua.petbed.adoption.domain.repository.AdoptionViewHistoryRepository;
import op.edu.ua.petbed.common.dto.*;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.pet.PetService;
import op.edu.ua.petbed.user.UserService;
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

        return AdoptionPostDTO.fromEntity(saved, pet.name(), pet.photoId(), pet.breed(), pet.color(),
                pet.age(), pet.sex(), pet.size(), pet.specialMarks());
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
                .map(r -> mapToResponseDTO(r, pet.name()))
                .toList();

        return new AdoptionPostDetailDTO(
                AdoptionPostDTO.fromEntity(post, pet.name(), pet.photoId(), pet.breed(), pet.color(),
                        pet.age(), pet.sex(), pet.size(), pet.specialMarks()),
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

        adoptionResponseRepository.deleteByAdoptionPostId(postId);
        adoptionSavedPostRepository.deleteByPostId(postId);
        adoptionViewHistoryRepository.deleteByPostId(postId);
        adoptionPostRepository.delete(post);

        petService.updateStatus(post.getPetId(), PetStatus.DEFAULT);

        log.info("Cancelled adoption post: id={}, petId={}", postId, post.getPetId());
    }

    @Override
    public boolean existsByPetId(Long petId) {
        return adoptionPostRepository.existsByPetId(petId);
    }

    @Override
    public boolean existsByOwnerId(Long ownerId) {
        return adoptionPostRepository.existsByOwnerId(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public @Nullable AdoptionRecommendationDTO findNextUnviewed(Long userId) {
        var posts = adoptionPostRepository.findUnviewedActivePosts(userId, PageRequest.of(0, 1));

        if (posts.isEmpty()) {
            return null;
        }

        AdoptionPost post = posts.getContent().get(0);
        return mapToRecommendationDTO(post, userId);
    }

    private AdoptionRecommendationDTO mapToRecommendationDTO(AdoptionPost post, @Nullable Long userId) {
        PetDTO pet = petService.findById(post.getPetId());
        boolean isSaved = userId != null && adoptionSavedPostRepository.existsByPostIdAndUserId(post.getIdOrThrow(), userId);

        return new AdoptionRecommendationDTO(
                post.getIdOrThrow(),
                pet.id(),
                pet.name(),
                pet.photoId(),
                pet.breed(),
                pet.color(),
                pet.age(),
                pet.sex(),
                pet.size(),
                pet.specialMarks(),
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
    }

    @Override
    @Transactional
    public @Nullable AdoptionRecommendationDTO findPreviousFromHistory(Long userId) {
        log.debug("findPreviousFromHistory called for userId={}", userId);

        userService.incrementAdoptionHistoryOffset(userId);
        int currentOffset = userService.getAdoptionHistoryOffset(userId);
        log.debug("After increment: offset={}", currentOffset);

        var historyList = adoptionViewHistoryRepository
                .findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(currentOffset, 1));

        log.debug("History list size: {}", historyList.size());

        if (historyList.isEmpty()) {
            log.debug("History is empty, returning null");
            return null;
        }

        Long postId = historyList.get(0).getPostId();
        log.debug("Found postId from history: {}", postId);

        AdoptionRecommendationDTO post = findById(postId, userId);

        if (post == null) {
            log.debug("Post not found (deleted), trying next");
            return findPreviousFromHistory(userId);
        }

        log.debug("Returning post: {}", post.petName());
        return post;
    }

    private @Nullable AdoptionRecommendationDTO findById(Long postId, Long userId) {
        return adoptionPostRepository.findById(postId)
                .map(post -> mapToRecommendationDTO(post, userId))
                .orElse(null);
    }

    private AdoptionPostDTO mapToPostDTO(AdoptionPost post) {
        PetDTO pet = petService.findById(post.getPetId());
        return AdoptionPostDTO.fromEntity(post, pet.name(), pet.photoId(), pet.breed(), pet.color(),
                pet.age(), pet.sex(), pet.size(), pet.specialMarks());
    }

    private AdoptionResponseDTO mapToResponseDTO(AdoptionResponse response, String postPetName) {
        var responder = userService.findById(response.getResponderId());

        return AdoptionResponseDTO.fromEntity(
                response,
                responder.telegramUsername(),
                responder.telegramUsername(),
                postPetName
        );
    }

    @Override
    @Transactional(readOnly = true)
    public @Nullable AdoptionRecommendationDTO findByIdAsRecommendation(Long postId) {
        var post = adoptionPostRepository.findById(postId).orElse(null);
        return post != null ? mapToRecommendationDTO(post, null) : null;
    }
}
