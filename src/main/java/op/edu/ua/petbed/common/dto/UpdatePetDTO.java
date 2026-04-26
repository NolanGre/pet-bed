package op.edu.ua.petbed.common.dto;

import lombok.Builder;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@Builder
@NullMarked
public record UpdatePetDTO(
        Long id,
        @Nullable String name,
        @Nullable String photoId,
        @Nullable String breed,
        @Nullable String color,
        @Nullable String colorPattern,
        @Nullable Integer age,
        @Nullable PetSex sex,
        @Nullable PetSize size,
        @Nullable String specialMarks
) {
}