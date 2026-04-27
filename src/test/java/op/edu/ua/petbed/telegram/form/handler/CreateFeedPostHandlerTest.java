package op.edu.ua.petbed.telegram.form.handler;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.feed.FeedService;
import op.edu.ua.petbed.common.dto.CreateFeedPostDTO;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CreateFeedPostHandlerTest {

    @Mock
    FeedService feedService;

    @Mock
    UserService userService;

    @InjectMocks
    CreateFeedPostHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_validFormData_createsPostAndReturnsSuccess() {
            // given
            FormData data = getFormData();
            when(userService.findById(1L)).thenReturn(new UserDTO(1L, 123L, "username", UserType.VOLUNTEER, null));

            // when
            BotApiMethod<?> result = underTest.handle(data);

            // then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage message = (SendMessage) result;
            assertThat(message.getText()).contains("Публікацію створено");
            assertThat(message.getChatId()).isEqualTo("123");

            ArgumentCaptor<CreateFeedPostDTO> dtoCaptor = ArgumentCaptor.forClass(CreateFeedPostDTO.class);
            verify(feedService).create(dtoCaptor.capture());

            CreateFeedPostDTO capturedDto = dtoCaptor.getValue();
            assertThat(capturedDto.publisherId()).isEqualTo(1L);
            assertThat(capturedDto.text()).isEqualTo("Test post text");
            assertThat(capturedDto.photoUrl()).isEqualTo("photo_file_id");
            assertThat(capturedDto.latitude()).isEqualTo(50.45);
            assertThat(capturedDto.longitude()).isEqualTo(30.52);
        }

        @Test
        void handle_userNotFound_throwsException() {
            // given
            FormData data = getFormData();
            when(userService.findById(1L))
                    .thenThrow(new PetBedException("User not found", PetBedException.ErrorCode.USER_NOT_FOUND));

            // then
            assertThatThrownBy(() -> underTest.handle(data))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.USER_NOT_FOUND);
        }

        private static @NonNull FormData getFormData() {
            List<FormStep> steps = FormType.CREATE_FEED_POST.steps();
            var answers = new LinkedHashMap<FormStep, FormInput>();
            answers.put(steps.get(0), new FormInput.Text("Test post text"));
            answers.put(steps.get(1), new FormInput.Photo("photo_file_id"));
            answers.put(steps.get(2), new FormInput.Location(50.45, 30.52));

            return new FormData(1L, 123L, CallbackId.FEED, answers);
        }
    }
}