package op.edu.ua.petbed.common.dto;

import op.edu.ua.petbed.common.model.PetSex;
import op.edu.ua.petbed.common.model.PetSize;
import op.edu.ua.petbed.common.model.PetStatus;
import op.edu.ua.petbed.common.model.PetType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record PetDTO(
        Long id,
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
        String specialMarks,
        PetStatus status) {
}