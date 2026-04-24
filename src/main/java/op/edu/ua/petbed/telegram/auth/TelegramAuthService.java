package op.edu.ua.petbed.telegram.auth;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@NullMarked
@Service
@RequiredArgsConstructor
public class TelegramAuthService {

    private final UserService userService;

    public UserAuthContext authenticate(Long telegramId, String telegramUsername) {
        UserDTO user = userService.registerOrGet(telegramId, telegramUsername);
        return new UserAuthContext(user.id(), user.id(), user.type(), user.telegramUsername());
    }

    public UserAuthContext requireVolunteer(Long telegramId, String telegramUsername) {
        UserAuthContext auth = authenticate(telegramId, telegramUsername);
        if (!auth.isVolunteer()) {
            throw new PetBedException("This action requires volunteer status", PetBedException.ErrorCode.AUTHORIZATION_REQUIRED);
        }
        return auth;
    }
}