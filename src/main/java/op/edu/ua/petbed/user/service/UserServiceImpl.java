package op.edu.ua.petbed.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.user.UserService;
import op.edu.ua.petbed.user.model.User;
import op.edu.ua.petbed.user.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    @Override
    public UserDTO registerOrGet(@Nullable Long telegramId, @Nullable String telegramUsername) {
        validateOrThrow(telegramId, telegramUsername);

        User user = userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    User newUser = userRepository.save(User.create(telegramId, telegramUsername));
                    log.info("Created new user: id={}, telegramId={}", newUser.getIdOrThrow(), telegramId);
                    return newUser;
                });
        
        log.debug("Retrieved existing user: id={}, telegramId={}", user.getIdOrThrow(), telegramId);
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