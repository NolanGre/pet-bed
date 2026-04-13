package op.edu.ua.petbed.user.service;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.user.model.User;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @InjectMocks
    UserServiceImpl underTest;

    // .registerOrGet() -------------------------------------------------

    @Test
    void registerOrGet_existing_user_returns_dto() {
        // given
        User existing = User.create(123L, "john_doe");
        ReflectionTestUtils.setField(existing, "id", 1L);
        given(userRepository.findByTelegramId(123L)).willReturn(Optional.of(existing));

        // when
        UserDTO result = underTest.registerOrGet(123L, "john_doe");

        // then
        assertThat(result.telegramId()).isEqualTo(123L);
        assertThat(result.telegramUsername()).isEqualTo("john_doe");
        assertThat(result.type()).isEqualTo(UserType.REGULAR);
        verify(userRepository).findByTelegramId(123L);
        verify(userRepository, never()).save(any());
    }

    @Test
    void registerOrGet_new_user_creates_and_returns_dto() {
        // given
        User created = User.create(123L, "john_doe");
        ReflectionTestUtils.setField(created, "id", 1L);
        given(userRepository.findByTelegramId(123L)).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willReturn(created);

        // when
        UserDTO result = underTest.registerOrGet(123L, "john_doe");

        // then
        assertThat(result.telegramId()).isEqualTo(123L);
        assertThat(result.telegramUsername()).isEqualTo("john_doe");
        assertThat(result.type()).isEqualTo(UserType.REGULAR);
        verify(userRepository).findByTelegramId(123L);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void registerOrGet_null_telegramId_throws_PetBedException() {
        // when / then
        assertThatThrownBy(() -> underTest.registerOrGet(null, "john_doe"))
                .isInstanceOf(PetBedException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.USER_TELEGRAM_ID_REQUIRED);
        verifyNoInteractions(userRepository);
    }

    @Test
    void registerOrGet_null_telegramUsername_throws_PetBedException() {
        // when / then
        assertThatThrownBy(() -> underTest.registerOrGet(123L, null))
                .isInstanceOf(PetBedException.class)
                .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.USER_TELEGRAM_USERNAME_REQUIRED);
        verifyNoInteractions(userRepository);
    }
}