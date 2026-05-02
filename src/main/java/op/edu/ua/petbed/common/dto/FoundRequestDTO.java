package op.edu.ua.petbed.common.dto;

import op.edu.ua.petbed.common.model.PetType;
import org.jspecify.annotations.NullMarked;
import org.locationtech.jts.geom.Point;

import java.time.Instant;

/**
 * DTO for FoundRequest entity.
 * Located in common/dto for Spring Modulith compliance.
 */
import org.jspecify.annotations.Nullable;

@NullMarked
public record FoundRequestDTO(
        Long id,
        Long finderId,
        String photoUrl,
        PetType petType,
        Point location,
        @Nullable String breedText,
        @Nullable String colorText,
        @Nullable String coatText,
        @Nullable String sizeText,
        @Nullable String sexText,
        @Nullable String featuresText,
        Instant createdAt
) {
}
