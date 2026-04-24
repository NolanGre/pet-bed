package op.edu.ua.petbed.telegram.callback.handler.profile;

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

@ExtendWith(MockitoExtension.class)
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class ProfileChangeTypeHandlerTest {

    @Mock
    CallbackQueryContext context;

    @InjectMocks
    ProfileChangeTypeHandler underTest;

    @Nested
    @DisplayName("getCallbackId()")
    class GetCallbackId {

        @Test
        void returns_PROFILE_CHANGE_TYPE() {
            CallbackId result = underTest.getCallbackId();
            assertThat(result).isEqualTo(CallbackId.PROFILE_CHANGE_TYPE);
        }
    }

    @Nested
    @DisplayName("handle(CallbackQueryContext)")
    class HandleMethod {

        @Test
        void returns_message_with_profile_type_description() {
            // given
            Long chatId = 123L;
            Long userId = 456L;
            Integer messageId = 1;

            given(context.chatId()).willReturn(chatId);
            given(context.userTelegramId()).willReturn(userId);
            given(context.messageId()).willReturn(messageId);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            assertThat(result).isInstanceOf(EditMessageText.class);
            EditMessageText editMessage = (EditMessageText) result;

            assertThat(editMessage.getText()).contains("Зміна типу профілю");
            assertThat(editMessage.getText()).contains("Звичайний");
            assertThat(editMessage.getText()).contains("Волонтер");
        }

        @Test
        void returns_message_with_volunteer_benefits() {
            // given
            Long chatId = 123L;
            Long userId = 456L;
            Integer messageId = 1;

            given(context.chatId()).willReturn(chatId);
            given(context.userTelegramId()).willReturn(userId);
            given(context.messageId()).willReturn(messageId);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            EditMessageText editMessage = (EditMessageText) result;

            assertThat(editMessage.getText()).contains("Створення оголошень");
        }
    }

    @Nested
    @DisplayName("Keyboard")
    class KeyboardTests {

        @Test
        void keyboard_contains_confirm_change_type_button_with_entity_id() {
            // given
            Long chatId = 123L;
            Long userId = 456L;
            Integer messageId = 1;

            given(context.chatId()).willReturn(chatId);
            given(context.userTelegramId()).willReturn(userId);
            given(context.messageId()).willReturn(messageId);

            // when
            BotApiMethod<?> result = underTest.handle(context);

            // then
            EditMessageText editMessage = (EditMessageText) result;
            InlineKeyboardMarkup replyMarkup = editMessage.getReplyMarkup();

            assertThat(replyMarkup.getKeyboard()).isNotEmpty();

            // Find button with "✓ Підтвердження зміни типу"
            var hasConfirmButton = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .anyMatch(btn -> btn.getText().contains("✓ Підтвердити зміну"));

            assertThat(hasConfirmButton).isTrue();

            // Verify callback data contains PROFILE_CHANGE_TYPE_CONFIRM id (112) with entityId
            var hasCorrectCallback = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .anyMatch(btn -> btn.getCallbackData() != null
                            && btn.getCallbackData().startsWith("211," + userId + ","));

            assertThat(hasCorrectCallback).isTrue();
        }

        @Test
        void keyboard_contains_back_button_to_PROFILE() {
            // given
            Long chatId = 123L;
            Long userId = 456L;
            Integer messageId = 1;

            given(context.chatId()).willReturn(chatId);
            given(context.userTelegramId()).willReturn(userId);
            given(context.messageId()).willReturn(messageId);

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

            // Verify callback data contains PROFILE id (110) - parent
            String expectedCallbackPrefix = String.valueOf(CallbackId.PROFILE.id());
            var hasCorrectCallback = replyMarkup.getKeyboard().stream()
                    .flatMap(Collection::stream)
                    .anyMatch(btn -> btn.getCallbackData() != null
                            && btn.getCallbackData().startsWith(expectedCallbackPrefix + ","));

            assertThat(hasCorrectCallback).isTrue();
        }
    }
}