package op.edu.ua.petbed.user;

import op.edu.ua.petbed.common.dto.LocationDTO;
import op.edu.ua.petbed.common.dto.UserDTO;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;


@NullMarked
public interface UserService {
    UserDTO registerOrGet(Long telegramId, String telegramUsername);

    UserDTO findById(Long internalId);
    UserDTO toggleUserType(Long internalId);

    void setLocation(Long userId, double latitude, double longitude);

    @Nullable
    LocationDTO getLocation(Long userId);

    int getAdoptionHistoryOffset(Long userId);

    void incrementAdoptionHistoryOffset(Long userId);

    void resetAdoptionHistoryOffset(Long userId);
}