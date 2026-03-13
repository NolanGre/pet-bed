package op.edu.ua.petbed.common.exceptions;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WebhookExceptionHandler {

    public static BotApiMethod<?> handle(Update update, Exception e) {
        Long chatId = extractChatId(update);

        if (e instanceof PetBedException petBedException) {
            log.warn("Business error [{}]: {}", petBedException.getErrorCode().name(), petBedException.getMessage());
            return buildMessage(chatId, petBedException.getErrorCode().getUserMessage());
        }

        log.error("Unexpected error while processing update: {}", update.getUpdateId(), e);
        return buildMessage(chatId, PetBedException.ErrorCode.INTERNAL_ERROR.getUserMessage());
    }

    private static Long extractChatId(Update update) {
        if (update == null) {
            return null;
        }

        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        }

        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }

        return null;
    }

    private static SendMessage buildMessage(Long chatId, String text) {
        if (chatId == null) {
            log.error("Cannot send error message: chatId is null");
            return null;
        }

        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
    }
}
