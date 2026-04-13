package op.edu.ua.petbed.user.model;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.UserType;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UserTest {

    // .create() -------------------------------------------------

    @Test
    void create_whenValidArgs_thenReturnsUserWithRegularType() {
        // given
        Long telegramId = 123456789L;
        String username = "john_doe";

        // when
        User user = User.create(telegramId, username);

        // then
        assertThat(user.getType()).isEqualTo(UserType.REGULAR);
    }

    @Test
    void create_whenValidArgs_thenFieldsAreSet() {
        // given
        Long telegramId = 123456789L;
        String username = "john_doe";

        // when
        User user = User.create(telegramId, username);

        // then
        assertThat(user.getTelegramId()).isEqualTo(telegramId);
        assertThat(user.getTelegramUsername()).isEqualTo(username);
    }

    @Test
    void create_whenTelegramIdIsNull_thenThrowsPetBedException() {
        assertThatThrownBy(() -> User.create(null, "john_doe"))
                .isInstanceOf(PetBedException.class)
                .extracting(e -> ((PetBedException) e).getErrorCode())
                .isEqualTo(PetBedException.ErrorCode.USER_TELEGRAM_ID_REQUIRED);
    }

    @Test
    void create_whenTelegramUsernameIsNull_thenThrowsPetBedException() {
        // given / when / then
        assertThatThrownBy(() -> User.create(123456789L, null))
                .isInstanceOf(PetBedException.class)
                .extracting(e -> ((PetBedException) e).getErrorCode())
                .isEqualTo(PetBedException.ErrorCode.USER_TELEGRAM_USERNAME_REQUIRED);
    }

    // .switchType() -------------------------------------------------

    @Test
    void switchType_whenRegular_thenBecomesVolunteer() {
        // given
        User user = User.create(123L, "john_doe");

        // when
        user.switchType();

        // then
        assertThat(user.getType()).isEqualTo(UserType.VOLUNTEER);
    }

    @Test
    void switchType_whenVolunteer_thenBecomesRegular() {
        // given
        User user = User.create(123L, "john_doe");
        user.switchType();

        // when
        user.switchType();

        // then
        assertThat(user.getType()).isEqualTo(UserType.REGULAR);
    }

    @Test
    void switchType_whenCalledTwice_thenReturnsOriginalType() {
        // given
        User user = User.create(123L, "john_doe");
        UserType original = user.getType();

        // when
        user.switchType();
        user.switchType();

        // then
        assertThat(user.getType()).isEqualTo(original);
    }
}
