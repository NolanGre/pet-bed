package op.edu.ua.petbed.telegram.callback;

import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class CallbackQueryContextTest {

    @Test
    void from_validUpdate_returnsContext() {
        // given
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
        when(callbackQuery.getData()).thenReturn("{\"a\":\"CONFIRM\",\"p\":\"item1\"}");
        when(callbackQuery.getMessage()).thenReturn(message);

        Update update = mock(Update.class);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);

        // when
        CallbackQueryContext result = CallbackQueryContext.from(update);

        // then
        assertThat(result.callbackData().action()).isEqualTo("CONFIRM");
        assertThat(result.callbackData().payload()).isEqualTo("item1");
        assertThat(result.chatId()).isEqualTo(456L);
        assertThat(result.userId()).isEqualTo(123L);
        assertThat(result.username()).isEqualTo("testuser");
        assertThat(result.messageId()).isEqualTo(10);
        assertThat(result.update()).isEqualTo(update);
    }

    @Test
    void from_updateWithNullMessage_returnsNullChatId() {
        // given
        User user = mock(User.class);
        when(user.getId()).thenReturn(123L);
        when(user.getUserName()).thenReturn("testuser");

        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        when(callbackQuery.getFrom()).thenReturn(user);
        when(callbackQuery.getData()).thenReturn("{\"a\":\"CONFIRM\"}");
        when(callbackQuery.getMessage()).thenReturn(null);

        Update update = mock(Update.class);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);

        // when
        CallbackQueryContext result = CallbackQueryContext.from(update);

        // then
        assertThat(result.chatId()).isNull();
        assertThat(result.messageId()).isNull();
    }
}