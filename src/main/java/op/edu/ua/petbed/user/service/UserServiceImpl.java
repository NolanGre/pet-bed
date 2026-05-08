package op.edu.ua.petbed.user.service;

import java.util.Optional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.dto.LocationDTO;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.user.UserService;
import op.edu.ua.petbed.user.model.User;
import op.edu.ua.petbed.user.repository.UserRepository;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.locationtech.jts.geom.Point;
import org.springframework.stereotype.Service;

@NullMarked
@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

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
    public UserDTO findById(Long internalId) {
        User user = userRepository.findById(internalId)
                .orElseThrow(() -> new PetBedException("User not found with internalId: " + internalId, PetBedException.ErrorCode.USER_NOT_FOUND));
        return toDto(user);
    }

    @Override
    public UserDTO toggleUserType(Long internalId) {
        User user = userRepository.findById(internalId)
                .orElseThrow(() -> new PetBedException("User not found with internalId: " + internalId, PetBedException.ErrorCode.USER_NOT_FOUND));
        user.switchType();
        User saved = userRepository.save(user);
        log.info("User type toggled: telegramId={}, newType={}", internalId, user.getType());
        return toDto(saved);
    }

    @Override
    public void setLocation(Long userId, double latitude, double longitude) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PetBedException("User not found with internalId: " + userId, PetBedException.ErrorCode.USER_NOT_FOUND));
        user.setLocation(latitude, longitude);
        userRepository.save(user);
        log.info("User location updated: userId={}, lat={}, lon={}", userId, latitude, longitude);
    }

    @Override
    public @Nullable LocationDTO getLocation(Long userId) {
        return userRepository.findById(userId)
                .map(User::getLocation)
                .map(location -> new LocationDTO(location.getY(), location.getX()))
                .orElse(null);
    }

    @Override
    public int getAdoptionHistoryOffset(Long userId) {
        return userRepository.findById(userId)
                .map(User::getAdoptionHistoryOffset)
                .orElse(0);
    }

    @Override
    public void incrementAdoptionHistoryOffset(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PetBedException("User not found with internalId: " + userId, PetBedException.ErrorCode.USER_NOT_FOUND));
        user.setAdoptionHistoryOffset(user.getAdoptionHistoryOffset() + 1);
        userRepository.save(user);
    }

    @Override
    public void resetAdoptionHistoryOffset(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new PetBedException("User not found with internalId: " + userId, PetBedException.ErrorCode.USER_NOT_FOUND));
        user.setAdoptionHistoryOffset(0);
        userRepository.save(user);
    }

    private UserDTO toDto(User user) {
        return new UserDTO(
                user.getIdOrThrow(),
                user.getTelegramId(),
                user.getTelegramUsername(),
                user.getType(),
                user.getLocation()
        );
    }
}