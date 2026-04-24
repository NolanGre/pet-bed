package op.edu.ua.petbed.telegram.callback;

import op.edu.ua.petbed.common.exceptions.PetBedException;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CallbackQueryContextTest {

    @Nested
    class From {
        @Test
        void validUpdate_returnsContext() {
            User user = mock(User.class);
            when(user.getId()).thenReturn(123L);
            when(user.getUserName()).thenReturn("testuser");

            var chat = mock(org.telegram.telegrambots.meta.api.objects.chat.Chat.class);
            when(chat.getId()).thenReturn(456L);

            var message = mock(org.telegram.telegrambots.meta.api.objects.message.Message.class);
            when(message.getChatId()).thenReturn(456L);
            when(message.getMessageId()).thenReturn(10);

            CallbackQuery callbackQuery = mock(CallbackQuery.class);
            when(callbackQuery.getFrom()).thenReturn(user);
            when(callbackQuery.getData()).thenReturn("110,42,5");
            when(callbackQuery.getMessage()).thenReturn(message);

            Update update = mock(Update.class);
            when(update.getCallbackQuery()).thenReturn(callbackQuery);

            CallbackQueryContext result = CallbackQueryContext.from(update);

            assertThat(result.callbackData().callbackId()).isEqualTo(110);
            assertThat(result.callbackData().entityId()).isEqualTo(42L);
            assertThat(result.callbackData().offset()).isEqualTo(5);
            assertThat(result.callbackData().callbackId()).isEqualTo(110);
            assertThat(result.callbackData().entityId()).isEqualTo(42L);
            assertThat(result.callbackData().offset()).isEqualTo(5);
            assertThat(result.chatId()).isEqualTo(456L);
            assertThat(result.userTelegramId()).isEqualTo(123L);
            assertThat(result.username()).isEqualTo("testuser");
            assertThat(result.messageId()).isEqualTo(10);
            assertThat(result.update()).isEqualTo(update);
        }

        @Test
        void updateWithNullMessage_throws() {
            User user = mock(User.class);
            when(user.getId()).thenReturn(123L);
            when(user.getUserName()).thenReturn("testuser");

            CallbackQuery callbackQuery = mock(CallbackQuery.class);
            when(callbackQuery.getFrom()).thenReturn(user);
            when(callbackQuery.getData()).thenReturn("110,,");
            when(callbackQuery.getMessage()).thenReturn(null);

            Update update = mock(Update.class);
            when(update.getCallbackQuery()).thenReturn(callbackQuery);

            assertThatThrownBy(() -> CallbackQueryContext.from(update))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INTERNAL_ERROR);
        }

        @Test
        void emptyData_throws() {
            User user = mock(User.class);
            when(user.getId()).thenReturn(123L);

            CallbackQuery callbackQuery = mock(CallbackQuery.class);
            when(callbackQuery.getFrom()).thenReturn(user);
            when(callbackQuery.getData()).thenReturn("");

            Update update = mock(Update.class);
            when(update.getCallbackQuery()).thenReturn(callbackQuery);

            assertThatThrownBy(() -> CallbackQueryContext.from(update))
                    .isInstanceOf(PetBedException.class)
                    .hasFieldOrPropertyWithValue("errorCode", PetBedException.ErrorCode.INVALID_CALLBACK);
        }
    }
}
