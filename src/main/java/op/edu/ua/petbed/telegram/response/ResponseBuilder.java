package op.edu.ua.petbed.telegram.response;

import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

@NullMarked
public class ResponseBuilder {

    private ResponseBuilder() {}

    public static SendMessageBuilder sendMessage(Long chatId) {
        return new SendMessageBuilder(chatId);
    }

    public static EditMessageBuilder editMessage(Long chatId, Integer messageId) {
        return new EditMessageBuilder(chatId, messageId);
    }

    public static SendPhotoBuilder sendPhoto(Long chatId, String photoId) {
        return new SendPhotoBuilder(chatId, photoId);
    }

    public static EditPhotoBuilder editPhoto(Long chatId, Integer messageId, String photoId) {
        return new EditPhotoBuilder(chatId, messageId, photoId);
    }

    public static class SendMessageBuilder {
        private final Long chatId;
        private @Nullable String text;
        private @Nullable InlineKeyboardMarkup keyboard;

        private SendMessageBuilder(Long chatId) { this.chatId = chatId; }

        public SendMessageBuilder text(String text) { this.text = text; return this; }
        public SendMessageBuilder keyboard(InlineKeyboardMarkup keyboard) { this.keyboard = keyboard; return this; }

        public SendMessage build() {
            var builder = SendMessage.builder().chatId(chatId.toString());
            if (text != null) builder.text(text);
            if (keyboard != null) builder.replyMarkup(keyboard);
            return builder.build();
        }
    }

    public static class EditMessageBuilder {
        private final Long chatId;
        private final Integer messageId;
        private @Nullable String text;
        private @Nullable InlineKeyboardMarkup keyboard;

        private EditMessageBuilder(Long chatId, Integer messageId) {
            this.chatId = chatId;
            this.messageId = messageId;
        }

        public EditMessageBuilder text(String text) { this.text = text; return this; }
        public EditMessageBuilder keyboard(InlineKeyboardMarkup keyboard) { this.keyboard = keyboard; return this; }

        public EditMessageText build() {
            var builder = EditMessageText.builder().chatId(chatId.toString()).messageId(messageId);
            if (text != null) builder.text(text);
            if (keyboard != null) builder.replyMarkup(keyboard);
            return builder.build();
        }
    }

    public static class SendPhotoBuilder {
        private final Long chatId;
        private final String photoId;
        private @Nullable String caption;
        private @Nullable InlineKeyboardMarkup keyboard;

        private SendPhotoBuilder(Long chatId, String photoId) {
            this.chatId = chatId;
            this.photoId = photoId;
        }

        public SendPhotoBuilder caption(String caption) { this.caption = caption; return this; }
        public SendPhotoBuilder keyboard(InlineKeyboardMarkup keyboard) { this.keyboard = keyboard; return this; }

        public SendPhoto build() {
            var builder = SendPhoto.builder().chatId(chatId.toString()).photo(new InputFile(photoId));
            if (caption != null) builder.caption(caption);
            if (keyboard != null) builder.replyMarkup(keyboard);
            return builder.build();
        }
    }

    public static class EditPhotoBuilder {
        private final Long chatId;
        private final Integer messageId;
        private final String photoId;
        private @Nullable String caption;
        private @Nullable InlineKeyboardMarkup keyboard;

        private EditPhotoBuilder(Long chatId, Integer messageId, String photoId) {
            this.chatId = chatId;
            this.messageId = messageId;
            this.photoId = photoId;
        }

        public EditPhotoBuilder caption(String caption) { this.caption = caption; return this; }
        public EditPhotoBuilder keyboard(InlineKeyboardMarkup keyboard) { this.keyboard = keyboard; return this; }

        public EditMessageMedia build() {
            var media = new InputMediaPhoto(photoId);
            if (caption != null) media.setCaption(caption);
            var builder = EditMessageMedia.builder().chatId(chatId.toString()).messageId(messageId).media(media);
            if (keyboard != null) builder.replyMarkup(keyboard);
            return builder.build();
        }
    }
}