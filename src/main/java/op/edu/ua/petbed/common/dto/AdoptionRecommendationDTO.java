package op.edu.ua.petbed.common.dto;

import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
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
        Integer petAge,
        PetSex petSex,
        PetSize petSize,
        @Nullable String petSpecialMarks,
        @Nullable String ownerComment,
        @Nullable Instant createdAt,
        boolean isSaved
) {}
