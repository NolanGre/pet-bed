package op.edu.ua.petbed.adoption.dto;

import op.edu.ua.petbed.adoption.domain.model.AdoptionPost;
import op.edu.ua.petbed.adoption.domain.model.enums.AdoptionPostStatus;
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
        @Nullable String ownerComment,
        AdoptionPostStatus status,
        @Nullable Long pendingResponseId,
        @Nullable Instant createdAt
) {
    public static AdoptionPostDTO fromEntity(AdoptionPost entity, String petName, String petPhotoUrl,
                                             @Nullable String petBreed, @Nullable String petColor) {
        return new AdoptionPostDTO(
                entity.getIdOrThrow(),
                entity.getPetId(),
                petName,
                petPhotoUrl,
                petBreed,
                petColor,
                entity.getOwnerComment(),
                entity.getStatus(),
                entity.getPendingResponseId(),
                entity.getCreatedAt()
        );
    }
}
