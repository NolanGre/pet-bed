package op.edu.ua.petbed.telegram.callback.handler.profile;

import op.edu.ua.petbed.common.model.UserType;
import op.edu.ua.petbed.telegram.auth.TelegramAuthService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Collection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProfileCallbackHandlerTest {

    @Mock
    TelegramAuthService authService;

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    ProfileCallbackHandler underTest;

    @Nested
    @DisplayName("getCallbackId()")
    class GetCallbackId {

        @Test
        void returns_PROFILE() {
            CallbackId result = underTest.getCallbackId();
            assertThat(result).isEqualTo(CallbackId.PROFILE);
        }
    }

    @Nested
    @DisplayName("handle(CallbackQueryContext)")
    class HandleMethod {

        @Test
        void callsAuthService_authenticate() {
            // given
            Long userId = 456L;
            String username = "testuser";
            UserAuthContext authContext = new UserAuthContext(userId, UserType.REGULAR);

            given(context.userId()).willReturn(userId);
            given(context.username()).willReturn(username);
            given(authService.authenticate(userId, username)).willReturn(authContext);

            // when
            underTest.handle(context);

            // then
            verify(authService).authenticate(userId, username);
        }

        @Test
        void validContext_returnsUserInfo_withUsername() {
            // given
            Long chatId = 123L;
            Long userId = 456L;
            String username = "testuser";
            UserAuthContext authContext = new UserAuthContext(userId, UserType.VOLUNTEER);

            given(context.chatId()).willReturn(chatId);
            given(context.userId()).willReturn(userId);
            given(context.username()).willReturn(username);
            given(context.messageId()).willReturn(1);
            given(authService.authenticate(userId, username)).willReturn(authContext);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isNotNull();
            assertThat(result).isInstanceOf(EditMessageText.class);
            EditMessageText editMessage = (EditMessageText) result;

            assertThat(editMessage.getText()).contains(username);
            assertThat(editMessage.getText()).contains("Волонтер");
        }

        @Test
        void validContext_returnsUserInfo_withUserType() {
            // given
            Long chatId = 123L;
            Long userId = 456L;
            String username = "testuser";
            UserAuthContext authContext = new UserAuthContext(userId, UserType.REGULAR);

            given(context.chatId()).willReturn(chatId);
            given(context.userId()).willReturn(userId);
            given(context.username()).willReturn(username);
            given(context.messageId()).willReturn(1);
            given(authService.authenticate(userId, username)).willReturn(authContext);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            EditMessageText editMessage = (EditMessageText) result;
            assertThat(editMessage.getText()).contains("Звичайний");
        }
    }

    @Nested
    @DisplayName("Profile keyboard")
    class ProfileKeyboard {

        @Test
        void keyboardContainsChangeTypeButton() {
            // given
            Long chatId = 123L;
            Long userId = 456L;
            String username = "testuser";
            UserAuthContext authContext = new UserAuthContext(userId, UserType.REGULAR);

            given(context.chatId()).willReturn(chatId);
            given(context.userId()).willReturn(userId);
            given(context.username()).willReturn(username);
            given(context.messageId()).willReturn(1);
            given(authService.authenticate(userId, username)).willReturn(authContext);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            EditMessageText editMessage = (EditMessageText) result;
            InlineKeyboardMarkup replyMarkup = editMessage.getReplyMarkup();

            assertThat(replyMarkup.getKeyboard()).isNotEmpty();

            // Find button with "Змінити тип профілю"
            var hasChangeTypeButton = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .anyMatch(btn -> btn.getText().contains("Змінити тип профілю"));

            assertThat(hasChangeTypeButton).isTrue();

            // Verify callback data contains PROFILE_CHANGE_TYPE id (111)
            String expectedCallbackPrefix = String.valueOf(CallbackId.PROFILE_CHANGE_TYPE.id());
            var hasCorrectCallback = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .anyMatch(btn -> btn.getCallbackData() != null
                            && btn.getCallbackData().startsWith(expectedCallbackPrefix));

            assertThat(hasCorrectCallback).isTrue();
        }

        @Test
        void keyboardContainsBackButton() {
            // given
            Long chatId = 123L;
            Long userId = 456L;
            String username = "testuser";
            UserAuthContext authContext = new UserAuthContext(userId, UserType.REGULAR);

            given(context.chatId()).willReturn(chatId);
            given(context.userId()).willReturn(userId);
            given(context.username()).willReturn(username);
            given(context.messageId()).willReturn(1);
            given(authService.authenticate(userId, username)).willReturn(authContext);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            EditMessageText editMessage = (EditMessageText) result;
            InlineKeyboardMarkup replyMarkup = editMessage.getReplyMarkup();

            // Find button with "⬅️ Повернутись"
            var hasBackButton = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .anyMatch(btn -> btn.getText().contains("⬅️ Повернутись"));

            assertThat(hasBackButton).isTrue();

            // Verify callback data contains MENU id (100) - parent
            String expectedCallbackPrefix = String.valueOf(CallbackId.MENU.id());
            var hasCorrectCallback = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .anyMatch(btn -> btn.getCallbackData() != null
                            && btn.getCallbackData().startsWith(expectedCallbackPrefix));

            assertThat(hasCorrectCallback).isTrue();
        }
    }
}