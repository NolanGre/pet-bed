package op.edu.ua.petbed.adoption.dto;

import op.edu.ua.petbed.adoption.domain.model.AdoptionResponse;
import op.edu.ua.petbed.adoption.domain.model.enums.AdoptionResponseStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@NullMarked
public record AdoptionResponseDTO(
        Long id,
        Long postId,
        Long responderId,
        String responderUsername,
        String responderTelegramUsername,
        String comment,
        AdoptionResponseStatus status,
        @Nullable Instant createdAt
) {
    public static AdoptionResponseDTO fromEntity(AdoptionResponse entity, String responderUsername, String responderTelegramUsername) {
        return new AdoptionResponseDTO(
                entity.getIdOrThrow(),
                entity.getAdoptionPostId(),
                entity.getResponderId(),
                responderUsername,
                responderTelegramUsername,
                entity.getComment(),
                entity.getStatus(),
                entity.getCreatedAt()
        );
    }

    public String getStatusEmoji() {
        return switch (status) {
            case NEW -> "🆕";
            case CONFIRMED_BY_OWNER -> "⏳";
            case REJECTED_BY_OWNER -> "❌";
            case FINAL_CONFIRMED -> "✅";
            case CANCELLED -> "🚫";
        };
    }
}
