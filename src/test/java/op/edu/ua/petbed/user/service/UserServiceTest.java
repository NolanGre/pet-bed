package op.edu.ua.petbed.user.service;

import op.edu.ua.petbed.common.dto.LocationDTO;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.user.model.User;
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
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

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

    // .setLocation() -------------------------------------------------

    @Test
    void setLocation_existing_user_location_is_saved() {
        // given
        User user = User.create(123L, "john_doe");
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        underTest.setLocation(1L, 50.45, 30.52);

        // then
        verify(userRepository).findById(1L);
        verify(userRepository).save(user);
    }

    @Test
    void setLocation_user_not_found_throws_PetBedException() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> underTest.setLocation(99L, 50.45, 30.52))
            .isInstanceOf(PetBedException.class)
            .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.USER_NOT_FOUND);
    }

    // .getLocation() -------------------------------------------------

    @Test
    void getLocation_user_has_location_returns_location_dto() {
        // given
        User user = User.create(123L, "john_doe");
        ReflectionTestUtils.setField(user, "id", 1L);
        // User.setLocation creates a Point with (longitude, latitude) = (30.52, 50.45)
        user.setLocation(50.45, 30.52);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        LocationDTO result = underTest.getLocation(1L);

        // then
        LocationDTO nonNull = Objects.requireNonNull(result);
        assertThat(nonNull.latitude()).isEqualTo(50.45);
        assertThat(nonNull.longitude()).isEqualTo(30.52);
    }

    @Test
    void getLocation_user_has_no_location_returns_null() {
        // given
        User user = User.create(123L, "john_doe");
        ReflectionTestUtils.setField(user, "id", 1L);
        given(userRepository.findById(1L)).willReturn(Optional.of(user));

        // when
        LocationDTO result = underTest.getLocation(1L);

        // then
        assertThat(result).isNull();
    }

    @Test
    void getLocation_user_not_found_returns_null() {
        // given
        given(userRepository.findById(99L)).willReturn(Optional.empty());

        // when
        LocationDTO result = underTest.getLocation(99L);

        // then
        assertThat(result).isNull();
    }


}