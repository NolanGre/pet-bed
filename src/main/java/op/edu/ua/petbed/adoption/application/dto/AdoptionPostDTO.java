package op.edu.ua.petbed.adoption.application.dto;

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
        @Nullable String ownerComment,
        AdoptionPostStatus status,
        @Nullable Long pendingResponseId,
        @Nullable Instant createdAt
) {
    public static AdoptionPostDTO fromEntity(AdoptionPost entity, String petName, String petPhotoUrl) {
        return new AdoptionPostDTO(
                entity.getIdOrThrow(),
                entity.getPetId(),
                petName,
                petPhotoUrl,
                entity.getOwnerComment(),
                entity.getStatus(),
                entity.getPendingResponseId(),
                entity.getCreatedAt()
        );
    }
}
