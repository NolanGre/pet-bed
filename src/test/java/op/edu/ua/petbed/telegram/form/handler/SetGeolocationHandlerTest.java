package op.edu.ua.petbed.telegram.form.handler;

import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.form.FormData;
import op.edu.ua.petbed.telegram.form.scheme.FormInput;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.user.UserService;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.util.LinkedHashMap;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class SetGeolocationHandlerTest {

    @Mock
    UserService userService;

    @InjectMocks
    SetGeolocationHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_validFormData_setsLocationAndReturnsMessage() {
            // given
            FormData data = getFormData();

            // when
            BotApiMethod<?> result = underTest.handle(data);

            // then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage message = (SendMessage) result;
            assertThat(message.getText()).contains("Вашу геолокацію збережено");
            assertThat(message.getChatId()).isEqualTo("123");
            assertThat(message.getReplyMarkup()).isNotNull();

            verify(userService).setLocation(eq(1L), eq(50.45), eq(30.52));
        }

        private static @NonNull FormData getFormData() {
            List<FormStep> steps = FormType.SET_GEOLOCATION.steps();
            var answers = new LinkedHashMap<FormStep, FormInput>();
            answers.put(steps.get(0), new FormInput.Location(50.45, 30.52));

            return new FormData(1L, 123L, CallbackId.FEED, answers);
        }
    }
}