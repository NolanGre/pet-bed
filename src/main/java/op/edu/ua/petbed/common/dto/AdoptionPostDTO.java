package op.edu.ua.petbed.common.dto;

import op.edu.ua.petbed.adoption.domain.model.AdoptionPost;
import op.edu.ua.petbed.common.model.AdoptionPostStatus;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@NullMarked
public record AdoptionPostDTO(
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
        AdoptionPostStatus status,
        @Nullable Long pendingResponseId,
        @Nullable Instant createdAt
) {
    public static AdoptionPostDTO fromEntity(AdoptionPost entity, String petName, String petPhotoUrl,
                                             @Nullable String petBreed, @Nullable String petColor,
                                             Integer petAge, PetSex petSex, PetSize petSize,
                                             @Nullable String petSpecialMarks) {
        return new AdoptionPostDTO(
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
                entity.getCreatedAt()
        );
    }
}
