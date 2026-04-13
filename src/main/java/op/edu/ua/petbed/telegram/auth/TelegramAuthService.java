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

    public UserAuthContext authenticate(@Nullable Long telegramId, @Nullable String telegramUsername) {
        if (telegramId == null) {
            throw new PetBedException("Telegram ID is required", PetBedException.ErrorCode.USER_TELEGRAM_ID_REQUIRED);
        }
        UserDTO user = userService.registerOrGet(telegramId, telegramUsername != null ? telegramUsername : "unknown");
        return new UserAuthContext(user.id(), user.type());
    }

    public UserAuthContext requireVolunteer(@Nullable Long telegramId, @Nullable String telegramUsername) {
        UserAuthContext auth = authenticate(telegramId, telegramUsername);
        if (!auth.isVolunteer()) {
            throw new PetBedException("This action requires volunteer status", PetBedException.ErrorCode.AUTHORIZATION_REQUIRED);
        }
        return auth;
    }
}