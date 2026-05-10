package op.edu.ua.petbed.common.dto;

import op.edu.ua.petbed.fostering.domain.model.FosteringPost;
import op.edu.ua.petbed.fostering.domain.model.FosteringPostStatus;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@NullMarked
public record FosteringPostDTO(
        Long id,
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
        FosteringPostStatus status,
        @Nullable Long pendingResponseId,
        @Nullable Integer plannedDurationDays,
        @Nullable Long tempOwnerId,
        @Nullable Instant startedAt,
        @Nullable Instant expiresAt,
        @Nullable Instant createdAt
) {
    public static FosteringPostDTO fromEntity(FosteringPost entity, String petName, String petPhotoUrl,
                                             @Nullable String petBreed, @Nullable String petColor,
                                             Integer petAge, PetSex petSex, PetSize petSize,
                                             @Nullable String petSpecialMarks) {
        return new FosteringPostDTO(
                entity.getIdOrThrow(),
                entity.getPetId(),
                petName,
                petPhotoUrl,
                petBreed,
                petColor,
                petAge,
                petSex,
                petSize,
                petSpecialMarks,
                entity.getOwnerComment(),
                entity.getStatus(),
                entity.getPendingResponseId(),
                entity.getPlannedDurationDays(),
                entity.getTempOwnerId(),
                entity.getStartedAt(),
                entity.getExpiresAt(),
                entity.getCreatedAt()
        );
    }
}
