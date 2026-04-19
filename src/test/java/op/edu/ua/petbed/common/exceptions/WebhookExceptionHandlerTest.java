package op.edu.ua.petbed.common.exceptions;


import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class WebhookExceptionHandlerTest {

    @Test
    void handle_whenPetBedException_thenReturnsMessageWithUserMessage() {
        // given
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(42L);

        PetBedException exception = new PetBedException(
                "business error",
                PetBedException.ErrorCode.USER_TELEGRAM_ID_REQUIRED
        );

        // when
        BotApiMethod<?> result = WebhookExceptionHandler.handle(update, exception);

        // then
        assertThat(result).isInstanceOf(SendMessage.class);
        SendMessage sendMessage = (SendMessage) Objects.requireNonNull(result);
        assertThat(sendMessage.getChatId()).isEqualTo("42");
        assertThat(sendMessage.getText()).isEqualTo(PetBedException.ErrorCode.USER_TELEGRAM_ID_REQUIRED.getUserMessage());
    }

    @Test
    void handle_whenUnexpectedException_thenReturnsInternalErrorMessage() {
        // given
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(42L);

        Exception exception = new RuntimeException("unexpected");

        // when
        BotApiMethod<?> result = WebhookExceptionHandler.handle(update, exception);

        // then
        assertThat(result).isInstanceOf(SendMessage.class);
        SendMessage sendMessage = (SendMessage) Objects.requireNonNull(result);
        assertThat(sendMessage.getChatId()).isEqualTo("42");
        assertThat(sendMessage.getText()).isEqualTo(PetBedException.ErrorCode.INTERNAL_ERROR.getUserMessage());
    }

    @Test
    void handle_whenUpdateHasMessage_thenUsesChatIdFromMessage() {
        // given
        Update update = mock(Update.class);
        Message message = mock(Message.class);
        when(update.hasMessage()).thenReturn(true);
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(99L);

        // when
        BotApiMethod<?> result = WebhookExceptionHandler.handle(update, new RuntimeException());

        // then
        assertThat(result).isInstanceOf(SendMessage.class);
        assertThat(((SendMessage) Objects.requireNonNull(result)).getChatId()).isEqualTo("99");
    }

    @Test
    void handle_whenUpdateHasCallbackQuery_thenUsesChatIdFromCallbackQuery() {
        // given
        Update update = mock(Update.class);
        CallbackQuery callbackQuery = mock(CallbackQuery.class);
        Message callbackMessage = mock(Message.class);
        when(update.hasMessage()).thenReturn(false);
        when(update.hasCallbackQuery()).thenReturn(true);
        when(update.getCallbackQuery()).thenReturn(callbackQuery);
        when(callbackQuery.getMessage()).thenReturn(callbackMessage);
        when(callbackMessage.getChatId()).thenReturn(77L);

        // when
        BotApiMethod<?> result = WebhookExceptionHandler.handle(update, new RuntimeException());

        // then
        assertThat(result).isInstanceOf(SendMessage.class);
        assertThat(((SendMessage) Objects.requireNonNull(result)).getChatId()).isEqualTo("77");
    }

    @Test
    void handle_whenUpdateHasNoMessageAndNoCallbackQuery_thenReturnsNull() {
        // given
        Update update = mock(Update.class);
        when(update.hasMessage()).thenReturn(false);
        when(update.hasCallbackQuery()).thenReturn(false);

        // when
        BotApiMethod<?> result = WebhookExceptionHandler.handle(update, new RuntimeException());

        // then
        assertThat(result).isNull();
    }
}
