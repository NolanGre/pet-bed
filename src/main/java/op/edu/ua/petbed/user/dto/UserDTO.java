package op.edu.ua.petbed.user.dto;

import op.edu.ua.petbed.user.model.UserType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record UserDTO(
        Long id,
        Long telegramId,
        String telegramUsername,
        UserType type
) {}
