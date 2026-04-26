package op.edu.ua.petbed.common.dto;

import op.edu.ua.petbed.common.model.UserType;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Point;

@NullMarked
public record UserDTO(
        Long id,
        Long telegramId,
        String telegramUsername,
        UserType type,
        @Nullable Point location
) {
}