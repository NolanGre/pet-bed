package op.edu.ua.petbed.common.dto.lost;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.domain.model.FoundRequest;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@NullMarked
public record FoundRequestDTO(
        Long id,
        Long finderId,
        String photoUrl,
        PetType petType,
        Point location,
        String description,
        Instant createdAt
) {
    public static FoundRequestDTO fromEntity(FoundRequest entity) {
        if (entity.getCreatedAt() == null) {
            throw new PetBedException("Found request createdAt is null", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
        return new FoundRequestDTO(
                entity.getIdOrThrow(),
                entity.getFinderId(),
                entity.getPhotoUrl(),
                entity.getPetType(),
                entity.getLocation(),
                entity.getDescription(),
                entity.getCreatedAt()
        );
    }
}
