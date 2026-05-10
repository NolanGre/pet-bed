package op.edu.ua.petbed.common.dto;

import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@NullMarked
public record FosteringRecommendationDTO(
        Long postId,
        Long petId,
        String petName,
        String petPhotoUrl,
        @Nullable String petBreed,
        @Nullable String petColor,
        Integer petAge,
        PetSex petSex,
        PetSize petSize,
        @Nullable String petSpecialMarks,
        @Nullable String ownerComment,
        @Nullable Integer plannedDurationDays,
        @Nullable Instant createdAt,
        boolean isSaved
) {}
