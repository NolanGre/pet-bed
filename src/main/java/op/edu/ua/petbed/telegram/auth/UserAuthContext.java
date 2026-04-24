package op.edu.ua.petbed.telegram.auth;

import op.edu.ua.petbed.common.model.UserType;
import org.jspecify.annotations.NullMarked;

@NullMarked
public record UserAuthContext(
        Long userTelegramId,
        Long userInternalId,
        UserType userType,
        String username
) {
    public boolean isVolunteer() {
        return userType == UserType.VOLUNTEER;
    }
}