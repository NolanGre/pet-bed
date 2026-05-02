package op.edu.ua.petbed.lost.application.dto;

import lombok.Builder;
import op.edu.ua.petbed.common.model.PetType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Point;

/**
 * DTO for creating a new found request.
 * Uses Lombok Builder pattern for flexible object construction.
 */
@NullMarked
@Builder
public record CreateFoundRequestDTO(
        Long finderId,
        String photoUrl,
        PetType petType,
        Point location,
        @Nullable String breed,
        @Nullable String color,
        @Nullable String coat,
        @Nullable String size,
        @Nullable String sex,
        @Nullable String features
) {
}
