package op.edu.ua.petbed.telegram.auth;

import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.user.UserService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class TelegramAuthServiceTest {

    @Mock
    UserService userService;

    @InjectMocks
    TelegramAuthService underTest;

    @Test
    void authenticate_existingUser_returnsAuthContext() {
        // given
        Long telegramId = 123L;
        String username = "testuser";
        UserDTO userDTO = new UserDTO(1L, telegramId, username, UserType.REGULAR);
        given(userService.registerOrGet(telegramId, username)).willReturn(userDTO);

        // when
        UserAuthContext result = underTest.authenticate(telegramId, username);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userTelegramId()).isEqualTo(1L);
        assertThat(result.userType()).isEqualTo(UserType.REGULAR);
        verify(userService).registerOrGet(telegramId, username);
    }

    @Test
    void authenticate_newUser_createsAndReturns() {
        // given
        Long telegramId = 456L;
        String username = "newuser";
        UserDTO userDTO = new UserDTO(2L, telegramId, username, UserType.VOLUNTEER);
        given(userService.registerOrGet(telegramId, username)).willReturn(userDTO);

        // when
        UserAuthContext result = underTest.authenticate(telegramId, username);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userTelegramId()).isEqualTo(2L);
        assertThat(result.userType()).isEqualTo(UserType.VOLUNTEER);
    }

    @Test
    void requireVolunteer_volunteerUser_returnsContext() {
        // given
        Long telegramId = 123L;
        String username = "volunteer";
        UserDTO userDTO = new UserDTO(1L, telegramId, username, UserType.VOLUNTEER);
        given(userService.registerOrGet(telegramId, username)).willReturn(userDTO);

        // when
        UserAuthContext result = underTest.requireVolunteer(telegramId, username);

        // then
        assertThat(result).isNotNull();
        assertThat(result.userTelegramId()).isEqualTo(1L);
        assertThat(result.isVolunteer()).isTrue();
    }

    @Test
    void requireVolunteer_regularUser_throws() {
        // given
        Long telegramId = 123L;
        String username = "regular";
        UserDTO userDTO = new UserDTO(1L, telegramId, username, UserType.REGULAR);
        given(userService.registerOrGet(telegramId, username)).willReturn(userDTO);

        // when/then
        assertThatThrownBy(() -> underTest.requireVolunteer(telegramId, username))
                .isInstanceOf(PetBedException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.AUTHORIZATION_REQUIRED);
    }
}