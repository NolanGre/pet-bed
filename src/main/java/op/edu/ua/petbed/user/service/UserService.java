package op.edu.ua.petbed.user.service;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.user.dto.UserDTO;
import op.edu.ua.petbed.user.model.User;
import op.edu.ua.petbed.user.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@NullMarked
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public UserDTO registerOrGet(@Nullable Long telegramId, @Nullable String telegramUsername) {
        validateOrThrow(telegramId, telegramUsername);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> userRepository.save(User.create(telegramId, telegramUsername)));

        return toDto(user);
    }

    private void validateOrThrow(@Nullable Long telegramId, @Nullable String telegramUsername) {
        if (telegramId == null) {
            throw new PetBedException("telegramId must not be null", PetBedException.ErrorCode.USER_TELEGRAM_ID_REQUIRED);
        }
        if (telegramUsername == null) {
            throw new PetBedException("telegramUsername must not be null", PetBedException.ErrorCode.USER_TELEGRAM_USERNAME_REQUIRED);
        }
    }

    private UserDTO toDto(User user) {
        return new UserDTO(
                user.getIdOrThrow(),
                user.getTelegramId(),
                user.getTelegramUsername(),
                user.getType()
        );
    }
}
