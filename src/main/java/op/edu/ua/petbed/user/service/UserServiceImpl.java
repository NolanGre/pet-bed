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
public class UserServiceImpl implements UserService {  //TODO: add or update tests to check @Validation correctness

    private final UserRepository userRepository;

    @Override
    public UserDTO registerOrGet(Long telegramId, String telegramUsername) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseGet(() -> {
                    User newUser = userRepository.save(User.create(telegramId, telegramUsername));
                    log.info("Created new user: id={}, telegramId={}", newUser.getIdOrThrow(), telegramId);
                    return newUser;
                });
        
        log.debug("Retrieved existing user: id={}, telegramId={}", user.getIdOrThrow(), telegramId);
        return toDto(user);
    }

    @Override
    public UserDTO findByTelegramId(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new PetBedException("User not found with telegramId: " + telegramId, PetBedException.ErrorCode.USER_NOT_FOUND));
        return toDto(user);
    }

    @Override
    public UserDTO toggleUserType(Long telegramId) {
        User user = userRepository.findByTelegramId(telegramId)
                .orElseThrow(() -> new PetBedException("User not found with telegramId: " + telegramId, PetBedException.ErrorCode.USER_NOT_FOUND));
        user.switchType();
        User saved = userRepository.save(user);
        log.info("User type toggled: telegramId={}, newType={}", telegramId, user.getType());
        return toDto(saved);
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