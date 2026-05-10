package op.edu.ua.petbed.common.dto;

import op.edu.ua.petbed.fostering.domain.model.FosteringResponse;
import op.edu.ua.petbed.fostering.domain.model.FosteringResponseStatus;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import java.time.Instant;

@NullMarked
public record FosteringResponseDTO(
        Long id,
        Long postId,
        String postPetName,
        Long responderId,
        String responderUsername,
        String responderTelegramUsername,
        String comment,
        FosteringResponseStatus status,
        @Nullable Instant createdAt
) {
    public static FosteringResponseDTO fromEntity(FosteringResponse entity, String responderUsername, String responderTelegramUsername, String postPetName) {
        return new FosteringResponseDTO(
                entity.getIdOrThrow(),
                entity.getFosteringPostId(),
                postPetName,
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
