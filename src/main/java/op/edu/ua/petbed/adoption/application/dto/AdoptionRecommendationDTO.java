package op.edu.ua.petbed.adoption.application.dto;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

/**
 * DTO for displaying adoption posts in the feed.
 */
@NullMarked
public record AdoptionRecommendationDTO(
        Long postId,
        Long petId,
        String petName,
        String petPhotoUrl,
        String petBreed,
        String petColor,
        @Nullable String ownerComment,
        Instant createdAt,
        boolean isSaved
) {}
