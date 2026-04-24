package op.edu.ua.petbed.user;

import op.edu.ua.petbed.common.dto.UserDTO;
import org.jspecify.annotations.NullMarked;


@NullMarked
public interface UserService {
    UserDTO registerOrGet(Long telegramId, String telegramUsername);

    UserDTO findById(Long internalId);
    UserDTO toggleUserType(Long internalId);
}