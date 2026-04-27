package op.edu.ua.petbed.telegram.callback.handler.feed;

import op.edu.ua.petbed.common.dto.UserDTO;
import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.form.scheme.FormType;
import op.edu.ua.petbed.telegram.service.FormService;
import op.edu.ua.petbed.user.UserService;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class FeedCreateCallbackHandlerTest {

    @Mock
    UserService userService;

    @Mock
    FormService formService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    FeedCreateCallbackHandler underTest;

    @Nested
    class Handle {

        @Test
        void handle_volunteerUser_initiatesForm() {
            // given
            Long chatId = 123L;
            Long userId = 1L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.VOLUNTEER, "volunteer");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.FEED_CREATE, null, 0));
            given(userService.findById(userId)).willReturn(new UserDTO(userId, 123L, "volunteer", UserType.VOLUNTEER, null));
            given(formService.startCreateForm(FormType.CREATE_FEED_POST, CallbackId.FEED, userId, chatId)).willReturn(null);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isNull();
            verify(userService).findById(userId);
            verify(formService).startCreateForm(FormType.CREATE_FEED_POST, CallbackId.FEED, userId, chatId);
        }

        @Test
        void handle_regularUser_returnsError() {
            // given
            Long chatId = 123L;
            Long userId = 1L;
            UserAuthContext auth = new UserAuthContext(123L, userId, UserType.REGULAR, "regular");

            given(context.auth()).willReturn(auth);
            given(context.chatId()).willReturn(chatId);
            given(context.callbackData()).willReturn(CallbackData.of(CallbackId.FEED_CREATE, null, 0));
            given(userService.findById(userId)).willReturn(new UserDTO(userId, 123L, "regular", UserType.REGULAR, null));

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(SendMessage.class);
            SendMessage message = (SendMessage) result;
            assertThat(message.getText()).isEqualTo("Тільки волонтери можуть створювати публікації");
            assertThat(message.getChatId()).isEqualTo(chatId.toString());
            verify(userService).findById(userId);
        }
    }
}