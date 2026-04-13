package op.edu.ua.petbed.telegram.auth;

import org.jspecify.annotations.NullMarked;
import op.edu.ua.petbed.common.model.UserType;

@NullMarked
public record UserAuthContext(
    Long userId,
    UserType userType
) {
    public boolean isVolunteer() {
        return userType == UserType.VOLUNTEER;
    }
}