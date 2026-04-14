package op.edu.ua.petbed.telegram.response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ResponseBuilderTest {

    // build_send_message -------------------------------------------------

    @Test
    @DisplayName("Text only - returns SendMessage")
    void build_text_only_returns_SendMessage() {
        BotApiMethod<?> result = ResponseBuilder.telegram()
                .chatId(123L)
                .text("Hello")
                .build();

        assertThat(result).isInstanceOf(SendMessage.class);
        SendMessage message = (SendMessage) result;
        assertThat(message.getChatId()).isEqualTo("123");
        assertThat(message.getText()).isEqualTo("Hello");
    }

    @Test
    @DisplayName("With keyboard - includes reply markup")
    void build_with_keyboard_includes_reply_markup() {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text("Button")
                .callbackData("DATA")
                .build();
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(List.of(button))));

        BotApiMethod<?> result = ResponseBuilder.telegram()
                .chatId(456L)
                .text("Choose:")
                .keyboard(keyboard)
                .build();

        assertThat(result).isInstanceOf(SendMessage.class);
        SendMessage message = (SendMessage) result;
        assertThat(message.getReplyMarkup()).isNotNull();
    }

    // build_edit_message -------------------------------------------------

    @Test
    @DisplayName("Edit message - returns EditMessageText")
    void build_edit_message_returns_EditMessageText() {
        BotApiMethod<?> result = ResponseBuilder.telegram()
                .chatId(123L)
                .editMessage(10)
                .text("Updated")
                .build();

        assertThat(result).isInstanceOf(EditMessageText.class);
        EditMessageText editMessage = (EditMessageText) result;
        assertThat(editMessage.getChatId()).isEqualTo("123");
        assertThat(editMessage.getMessageId()).isEqualTo(10);
        assertThat(editMessage.getText()).isEqualTo("Updated");
    }

    @Test
    @DisplayName("Edit with keyboard - includes reply markup")
    void build_edit_with_keyboard_includes_reply_markup() {
        InlineKeyboardButton button = InlineKeyboardButton.builder()
                .text("Button")
                .callbackData("DATA")
                .build();
        InlineKeyboardMarkup keyboard = new InlineKeyboardMarkup(List.of(new InlineKeyboardRow(List.of(button))));

        BotApiMethod<?> result = ResponseBuilder.telegram()
                .chatId(456L)
                .editMessage(20)
                .text("Updated:")
                .keyboard(keyboard)
                .build();

        assertThat(result).isInstanceOf(EditMessageText.class);
        EditMessageText editMessage = (EditMessageText) result;
        assertThat(editMessage.getReplyMarkup()).isNotNull();
    }
}