package op.edu.ua.petbed.common.dto;

import op.edu.ua.petbed.common.model.PetType;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

/**
 * DTO for FoundRequest entity.
 * Located in common/dto for Spring Modulith compliance.
 */
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
}
