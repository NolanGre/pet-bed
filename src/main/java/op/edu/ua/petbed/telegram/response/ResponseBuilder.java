package op.edu.ua.petbed.telegram.response;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.botapimethods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

import java.util.Objects;

/**
 * Builder for Telegram Bot API responses.
 * <p>
 * Supports building:
 * <ul>
 *   <li>SendMessage - text messages</li>
 *   <li>SendPhoto - photo messages with optional caption</li>
 *   <li>EditMessageText - editing existing message</li>
 * </ul>
 * <p>
 */
@NullMarked
public class ResponseBuilder {

    private ResponseBuilder() {
    }

    public static TelegramMessageBuilder telegram() {
        return new TelegramMessageBuilder();
    }

    public static class TelegramMessageBuilder {

        private @Nullable Long chatId;
        private @Nullable Integer messageId;
        private @Nullable String text;
        private @Nullable String photo;
        private @Nullable InlineKeyboardMarkup keyboard;

        public TelegramMessageBuilder chatId(Long chatId) {
            this.chatId = chatId;
            return this;
        }

        public TelegramMessageBuilder editMessage(Integer messageId) {
            this.messageId = messageId;
            return this;
        }

        public TelegramMessageBuilder text(String text) {
            this.text = text;
            return this;
        }

        public TelegramMessageBuilder photo(String photo) {
            this.photo = photo;
            return this;
        }

        public TelegramMessageBuilder keyboard(InlineKeyboardMarkup keyboard) {
            this.keyboard = keyboard;
            return this;
        }

        public BotApiMethod<?> build() {
            if (messageId != null) {
                return buildEditMessageText();
            }
            return buildSendMessage();
        }

        public PartialBotApiMethod<?> buildPhoto() {
            if (messageId != null) {
                return buildEditMessageMedia();
            }
            return buildSendPhoto();
        }

        private SendPhoto buildSendPhoto() {
            SendPhoto.SendPhotoBuilder builder = SendPhoto.builder();
            if (chatId != null) builder.chatId(chatId.toString());
            builder.photo(new InputFile(photo));
            if (text != null) builder.caption(text);
            if (keyboard != null) builder.replyMarkup(keyboard);
            return builder.build();
        }

        private EditMessageMedia buildEditMessageMedia() {
            InputMediaPhoto media = new InputMediaPhoto(Objects.requireNonNull(photo));
            if (text != null) media.setCaption(text);

            EditMessageMedia.EditMessageMediaBuilder builder = EditMessageMedia.builder();
            if (chatId != null) builder.chatId(chatId.toString());
            if (messageId != null) builder.messageId(messageId);
            builder.media(media);
            if (keyboard != null) builder.replyMarkup(keyboard);
            return builder.build();
        }

        private BotApiMethod<?> buildSendMessage() {
            SendMessage.SendMessageBuilder builder = SendMessage.builder();
            if (chatId != null) builder.chatId(chatId.toString());
            if (text != null) builder.text(text);
            if (keyboard != null) builder.replyMarkup(keyboard);
            return builder.build();
        }

        private BotApiMethod<?> buildEditMessageText() {
            EditMessageText.EditMessageTextBuilder builder = EditMessageText.builder();
            if (chatId != null) builder.chatId(chatId.toString());
            if (messageId != null) builder.messageId(messageId);
            if (text != null) builder.text(text);
            if (keyboard != null) builder.replyMarkup(keyboard);
            return builder.build();
        }
    }
}
