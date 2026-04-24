package op.edu.ua.petbed.common.dto;

import lombok.Builder;
import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetType;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Builder
public record CreatePetDTO(
        Long ownerId,
        String name,
        PetType type,
        String photoId,
        String breed,
        String color,
        String colorPattern,
        Integer age,
        PetSex sex,
        PetSize size,
        String specialMarks
) {
}