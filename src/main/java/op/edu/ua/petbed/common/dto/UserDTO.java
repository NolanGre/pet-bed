package op.edu.ua.petbed.common.dto;

import op.edu.ua.petbed.common.model.UserType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record UserDTO(
        Long id,
        Long telegramId,
        String telegramUsername,
        UserType type
) {}