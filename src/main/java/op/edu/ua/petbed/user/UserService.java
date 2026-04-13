package op.edu.ua.petbed.user;

import op.edu.ua.petbed.common.dto.UserDTO;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
public interface UserService {

    UserDTO registerOrGet(@Nullable Long telegramId, @Nullable String telegramUsername);
}