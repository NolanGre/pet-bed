package op.edu.ua.petbed.telegram.testutil;

import org.jspecify.annotations.NonNull;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.api.objects.chat.Chat;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import java.util.List;

public class TelegramUpdateFixtureUtil {

    public static Update withCommand(String text, Long userId, String username) {
        return withCommand(text, userId, username, 123L);
    }

    public static Update withCommand(String text, Long userId, String username, Long chatId) {
        int commandLength = text.indexOf(' ') > 0 ? text.indexOf(' ') : text.length();

        MessageEntity commandEntity = MessageEntity.builder()
                .type("bot_command")
                .offset(0)
                .length(commandLength)
                .build();

        User user = User.builder()
                .id(userId)
                .userName(username)
                .firstName("Test")
                .isBot(false)
                .build();

        Chat chat = Chat.builder()
                .id(chatId)
                .type("private")
                .build();

        Message message = Message.builder()
                .messageId(1)
                .from(user)
                .chat(chat)
                .text(text)
                .entities(List.of(commandEntity))
                .build();

        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    public static Update withCallback(String callbackData, Long userId) {
        return withCallback(callbackData, userId, "testuser", 123L, 1);
    }

    public static Update withCallback(String callbackData, Long userId, String username, Long chatId, Integer messageId) {
        User user = User.builder()
                .id(userId)
                .userName(username)
                .firstName("Test")
                .isBot(false)
                .build();

        Chat chat = Chat.builder()
                .id(chatId)
                .type("private")
                .build();

        Message message = Message.builder()
                .messageId(messageId)
                .from(user)
                .chat(chat)
                .text("Test message")
                .build();

        // Use constructor but need to use setter for data (constructor doesn't set it)
        return getUpdate(callbackData, user, message);
    }

    private static @NonNull Update getUpdate(String callbackData, User user, Message message) {
        CallbackQuery callbackQuery = new CallbackQuery(
                "callback-id",
                user,
                message,
                null,  // data - constructor doesn't set this, use setter below
                null,          // gameShortName
                null,          // chatInstance
                null           // payload
        );
        callbackQuery.setData(callbackData);  // Set data via setter

        Update update = new Update();
        update.setCallbackQuery(callbackQuery);
        return update;
    }

    public static Update withPaginationCallback(int offset, String payload, Long userId) {
        String data = String.format("{\"a\":\"PAGINATION\",\"p\":\"%s\",\"o\":%d}", payload, offset);
        return withCallback(data, userId);
    }

    public static Update withTextMessage(String text, Long userId, Long chatId) {
        User user = User.builder()
                .id(userId)
                .userName("testuser")
                .firstName("Test")
                .isBot(false)
                .build();

        Chat chat = Chat.builder()
                .id(chatId)
                .type("private")
                .build();

        Message message = Message.builder()
                .messageId(1)
                .from(user)
                .chat(chat)
                .text(text)
                .build();

        Update update = new Update();
        update.setMessage(message);
        return update;
    }

    public static Update withCallbackAction(String action, Long userId) {
        return withCallback(action, userId);
    }
}
