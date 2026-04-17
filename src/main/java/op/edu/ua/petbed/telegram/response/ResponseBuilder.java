package op.edu.ua.petbed.telegram.response;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Builder for Telegram Bot API responses.
 * <p>
 * Supports building:
 * <ul>
 *   <li>SendMessage - text messages</li>
 *   <li>EditMessageText - editing existing message</li>
 * </ul>
 * <p>
 */
@NullMarked
public class ResponseBuilder {

    private ResponseBuilder() {
    }

    /**
     * Creates a new ResponseBuilder instance.
     */
    public static TelegramMessageBuilder telegram() {
        return new TelegramMessageBuilder();
    }

    /**
     * Fluent builder for Telegram API methods.
     */
    public static class TelegramMessageBuilder {

        private @Nullable Long chatId;
        private @Nullable Integer messageId;
        private @Nullable String text;
        private @Nullable InlineKeyboardMarkup keyboard;

        /**
         * Sets the target chat ID.
         */
        public TelegramMessageBuilder chatId(Long chatId) {
            this.chatId = chatId;
            return this;
        }

        /**
         * Sets the message ID for editing (when responding to callbacks).
         */
        public TelegramMessageBuilder editMessage(Integer messageId) {
            this.messageId = messageId;
            return this;
        }

        /**
         * Sets the message text.
         */
        public TelegramMessageBuilder text(String text) {
            this.text = text;
            return this;
        }

        /**
         * Sets the inline keyboard markup.
         */
        public TelegramMessageBuilder keyboard(InlineKeyboardMarkup keyboard) {
            this.keyboard = keyboard;
            return this;
        }

        /**
         * Builds the appropriate Telegram API method based on the configuration.
         *
         * @return BotApiMethod<?> - SendMessage or EditMessageText
         */
        public BotApiMethod<?> build() {
            // Edit mode - editing existing message
            if (messageId != null) {
                return buildEditMessageText();
            }

            // New message
            return buildSendMessage();
        }

        private BotApiMethod<?> buildSendMessage() {
            SendMessage.SendMessageBuilder builder = SendMessage.builder();

            if (chatId != null) {
                builder.chatId(chatId.toString());
            }
            if (text != null) {
                builder.text(text);
            }
            if (keyboard != null) {
                builder.replyMarkup(keyboard);
            }

            return builder.build();
        }

        private BotApiMethod<?> buildEditMessageText() {
            EditMessageText.EditMessageTextBuilder builder = EditMessageText.builder();

            if (chatId != null) {
                builder.chatId(chatId.toString());
            }
            if (messageId != null) {
                builder.messageId(messageId);
            }
            if (text != null) {
                builder.text(text);
            }
            if (keyboard != null) {
                builder.replyMarkup(keyboard);
            }

            return builder.build();
        }
    }
}