package op.edu.ua.petbed.lost.application.dto;

import lombok.Builder;
import op.edu.ua.petbed.common.model.PetType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Point;

/**
 * DTO for creating a new lost request.
 * Uses Lombok Builder pattern for flexible object construction.
 */
@NullMarked
@Builder
public record CreateLostRequestDTO(
        Long petId,
        String contactInfo,
        Point location,
        PetType petType,
        @Nullable String breedText,
        @Nullable String colorText,
        @Nullable String coatText,
        @Nullable String sizeText,
        @Nullable String sexText,
        @Nullable String featuresText
) {
}
