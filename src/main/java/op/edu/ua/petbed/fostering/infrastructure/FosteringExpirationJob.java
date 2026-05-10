package op.edu.ua.petbed.fostering.infrastructure;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.fostering.domain.model.FosteringPost;
import op.edu.ua.petbed.fostering.domain.repository.FosteringPostRepository;
import op.edu.ua.petbed.pet.PetService;
import org.jspecify.annotations.NullMarked;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringExpirationJob {

    private final FosteringPostRepository fosteringPostRepository;
    private final PetService petService;

    @Scheduled(fixedRate = 3600000) // every hour
    @Transactional
    public void processExpiredFosterings() {
        log.info("Starting processing of expired fosterings");

        try {
            Instant now = Instant.now();
            List<FosteringPost> expired = fosteringPostRepository.findExpired(now);

            if (expired.isEmpty()) {
                log.debug("No expired fostering posts found");
                return;
            }

            log.info("Found {} expired fostering posts to process", expired.size());

            for (FosteringPost post : expired) {
                post.returnToOwner();
                fosteringPostRepository.save(post);
                petService.updateStatus(post.getPetId(), PetStatus.DEFAULT);

                long durationDays = calculateDurationDays(post);
                log.info("Fostering expired: postId={}, petId={}, tempOwnerId={}, duration={} days",
                        post.getIdOrThrow(), post.getPetId(), post.getTempOwnerId(), durationDays);
            }

            log.info("Successfully processed {} expired fostering posts", expired.size());
        } catch (Exception e) {
            log.error("Failed to process expired fosterings", e);
            // Don't re-throw to prevent scheduler from stopping
        }
    }

    private long calculateDurationDays(FosteringPost post) {
        if (post.getStartedAt() == null || post.getExpiresAt() == null) {
            return 0;
        }
        return Duration.between(post.getStartedAt(), post.getExpiresAt()).toDays();
    }
}