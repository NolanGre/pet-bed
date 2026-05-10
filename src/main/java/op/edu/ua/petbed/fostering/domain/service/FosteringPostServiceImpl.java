package op.edu.ua.petbed.fostering.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.fostering.FosteringPostService;
import op.edu.ua.petbed.fostering.domain.model.FosteringPost;
import op.edu.ua.petbed.fostering.domain.model.FosteringResponse;
import op.edu.ua.petbed.fostering.domain.model.FosteringViewHistory;
import op.edu.ua.petbed.fostering.domain.repository.FosteringPostRepository;
import op.edu.ua.petbed.fostering.domain.repository.FosteringResponseRepository;
import op.edu.ua.petbed.fostering.domain.repository.FosteringSavedPostRepository;
import op.edu.ua.petbed.fostering.domain.repository.FosteringViewHistoryRepository;
import op.edu.ua.petbed.common.dto.FosteringPostDTO;
import op.edu.ua.petbed.common.dto.FosteringPostDetailDTO;
import op.edu.ua.petbed.common.dto.FosteringRecommendationDTO;
import op.edu.ua.petbed.common.dto.FosteringResponseDTO;
import op.edu.ua.petbed.common.dto.PetDTO;
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
public class FosteringPostServiceImpl implements FosteringPostService {

    private final FosteringPostRepository fosteringPostRepository;
    private final FosteringResponseRepository fosteringResponseRepository;
    private final FosteringSavedPostRepository fosteringSavedPostRepository;
    private final FosteringViewHistoryRepository fosteringViewHistoryRepository;
    private final PetService petService;
    private final UserService userService;

    @Override
    @Transactional
    public FosteringPostDTO create(Long petId, Integer plannedDurationDays, @Nullable String ownerComment) {
        if (existsByPetId(petId)) {
            throw new PetBedException("Pet already has an active fostering post",
                    PetBedException.ErrorCode.ADOPTION_POST_ALREADY_EXISTS);
        }

        PetDTO pet = petService.findById(petId);

        FosteringPost post = FosteringPost.create(petId, plannedDurationDays, ownerComment);
        FosteringPost saved = fosteringPostRepository.save(post);

        petService.updateStatus(petId, PetStatus.IN_FOSTERING);

        log.info("Created fostering post: id={}, petId={}, duration={} days",
                saved.getIdOrThrow(), petId, plannedDurationDays);

        Instant createdAt = saved.getCreatedAt();
        if (createdAt == null) {
            throw new PetBedException("Fostering post createdAt is null",
                    PetBedException.ErrorCode.INTERNAL_ERROR);
        }

        return FosteringPostDTO.fromEntity(saved, pet.name(), pet.photoId(), pet.breed(), pet.color(),
                pet.age(), pet.sex(), pet.size(), pet.specialMarks());
    }

    @Override
    @Transactional(readOnly = true)
    public FosteringPostDetailDTO findById(Long id) {
        FosteringPost post = fosteringPostRepository.findById(id)
                .orElseThrow(() -> new PetBedException("Fostering post not found with id: " + id,
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());

        List<FosteringResponseDTO> responses = fosteringResponseRepository
                .findByPostIdOrderByStatusPriority(id)
                .stream()
                .map(r -> mapToResponseDTO(r, pet.name()))
                .toList();

        return new FosteringPostDetailDTO(
                FosteringPostDTO.fromEntity(post, pet.name(), pet.photoId(), pet.breed(), pet.color(),
                        pet.age(), pet.sex(), pet.size(), pet.specialMarks()),
                responses
        );
    }

    @Override
    @Transactional(readOnly = true)
    public Page<FosteringPostDTO> findAllByOwnerId(Long ownerId, Pageable pageable) {
        return fosteringPostRepository.findByOwnerId(ownerId, pageable)
                .map(this::mapToPostDTO);
    }

    @Override
    @Transactional
    public void cancel(Long postId, Long ownerId) {
        FosteringPost post = fosteringPostRepository.findById(postId)
                .orElseThrow(() -> new PetBedException("Fostering post not found with id: " + postId,
                        PetBedException.ErrorCode.ADOPTION_POST_NOT_FOUND));

        PetDTO pet = petService.findById(post.getPetId());
        if (!pet.ownerId().equals(ownerId)) {
            throw new PetBedException("Not authorized to cancel this post",
                    PetBedException.ErrorCode.ADOPTION_POST_NOT_AUTHORIZED);
        }

        fosteringResponseRepository.deleteByFosteringPostId(postId);
        fosteringSavedPostRepository.deleteByPostId(postId);
        fosteringViewHistoryRepository.deleteByPostId(postId);
        fosteringPostRepository.delete(post);

        petService.updateStatus(post.getPetId(), PetStatus.DEFAULT);

        log.info("Cancelled fostering post: id={}, petId={}", postId, post.getPetId());
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByPetId(Long petId) {
        return fosteringPostRepository.existsByPetId(petId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByOwnerId(Long ownerId) {
        return fosteringPostRepository.existsByOwnerId(ownerId);
    }

    @Override
    @Transactional(readOnly = true)
    public @Nullable FosteringRecommendationDTO findNextUnviewed(Long userId) {
        var posts = fosteringPostRepository.findUnviewedActivePosts(userId, PageRequest.of(0, 1));

        if (posts.isEmpty()) {
            return null;
        }

        FosteringPost post = posts.getContent().get(0);
        return mapToRecommendationDTO(post, userId);
    }

    private FosteringRecommendationDTO mapToRecommendationDTO(FosteringPost post, @Nullable Long userId) {
        PetDTO pet = petService.findById(post.getPetId());
        boolean isSaved = userId != null && fosteringSavedPostRepository.existsByPostIdAndUserId(post.getIdOrThrow(), userId);

        return new FosteringRecommendationDTO(
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
                post.getPlannedDurationDays(),
                post.getCreatedAt(),
                isSaved
        );
    }

    @Override
    @Transactional
    public void recordView(Long postId, Long userId) {
        if (!fosteringViewHistoryRepository.existsByPostIdAndUserId(postId, userId)) {
            FosteringViewHistory viewHistory = FosteringViewHistory.create(postId, userId);
            fosteringViewHistoryRepository.save(viewHistory);
        }
    }

    @Override
    @Transactional
    public @Nullable FosteringRecommendationDTO findPreviousFromHistory(Long userId) {
        log.debug("findPreviousFromHistory called for userId={}", userId);

        int currentOffset = userService.getFosteringHistoryOffset(userId);
        log.debug("Current offset: {}", currentOffset);

        var historyList = fosteringViewHistoryRepository
                .findByUserIdOrderByViewedAtDesc(userId, PageRequest.of(currentOffset, 1));

        log.debug("History list size: {}", historyList.size());

        if (historyList.isEmpty()) {
            log.debug("History is empty, returning null");
            return null;
        }

        Long postId = historyList.get(0).getPostId();
        log.debug("Found postId from history: {}", postId);

        FosteringRecommendationDTO post = findByIdAsRecommendation(postId);

        if (post == null) {
            log.debug("Post not found (deleted), trying next");
            return findPreviousFromHistory(userId);
        }

        userService.incrementFosteringHistoryOffset(userId);
        log.debug("Incremented offset after successful retrieval");

        log.debug("Returning post: {}", post.petName());
        return post;
    }

    @Override
    @Transactional(readOnly = true)
    public @Nullable FosteringRecommendationDTO findByIdAsRecommendation(Long postId) {
        var post = fosteringPostRepository.findById(postId).orElse(null);
        if (post == null || post.isCompleted()) {
            return null;
        }
        return mapToRecommendationDTO(post, null);
    }

    private FosteringPostDTO mapToPostDTO(FosteringPost post) {
        PetDTO pet = petService.findById(post.getPetId());
        return FosteringPostDTO.fromEntity(post, pet.name(), pet.photoId(), pet.breed(), pet.color(),
                pet.age(), pet.sex(), pet.size(), pet.specialMarks());
    }

    private FosteringResponseDTO mapToResponseDTO(FosteringResponse response, String postPetName) {
        var responder = userService.findById(response.getResponderId());

        return FosteringResponseDTO.fromEntity(
                response,
                responder.telegramUsername(),
                responder.telegramUsername(),
                postPetName
        );
    }
}
