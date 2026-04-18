package op.edu.ua.petbed.user;

import op.edu.ua.petbed.common.dto.UserDTO;
import org.jspecify.annotations.NullMarked;


@NullMarked
public interface UserService {
    UserDTO findByTelegramId(Long telegramId);

    UserDTO registerOrGet(Long telegramId, String telegramUsername);
    UserDTO toggleUserType(Long telegramId);
}