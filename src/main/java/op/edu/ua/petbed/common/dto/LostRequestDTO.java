package op.edu.ua.petbed.common.dto;

import op.edu.ua.petbed.common.model.PetType;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

/**
 * DTO for LostRequest entity.
 * Located in common/dto for Spring Modulith compliance.
 */
@NullMarked
public record LostRequestDTO(
        Long id,
        Long petId,
        String contactInfo,
        Point location,
        PetType petType,
        Instant createdAt
) {
}
