package op.edu.ua.petbed.lost.application.dto;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.PetType;
import op.edu.ua.petbed.lost.domain.model.LostRequest;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

@NullMarked
public record LostRequestDTO(
        Long id,
        Long petId,
        String contactInfo,
        Point lastSeenLocation,
        PetType petType,
        String searchText,
        Instant createdAt
) {
    public static LostRequestDTO fromEntity(LostRequest entity) {
        if (entity.getCreatedAt() == null) {
            throw new PetBedException("Lost request createdAt is null", PetBedException.ErrorCode.INTERNAL_ERROR);
        }
        return new LostRequestDTO(
                entity.getIdOrThrow(),
                entity.getPetId(),
                entity.getContactInfo(),
                entity.getLastSeenLocation(),
                entity.getPetType(),
                entity.getSearchText(),
                entity.getCreatedAt()
        );
    }
}
