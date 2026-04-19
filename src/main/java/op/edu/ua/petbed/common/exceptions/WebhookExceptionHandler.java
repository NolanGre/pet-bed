package op.edu.ua.petbed.common.exceptions;

import jakarta.validation.ConstraintViolationException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Slf4j
@NullMarked
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class WebhookExceptionHandler {

    public static @Nullable BotApiMethod<?> handle(Update update, Exception e) {
        Long chatId = extractChatId(update);
        if (chatId == null) {
            log.error("Cannot handle error response: chatId is null. Update: {}", update.getUpdateId(), e);
            return null;
        }

        if (e instanceof PetBedException petBedException) {
            log.warn("Business error [{}]: {}", petBedException.getErrorCode().name(), petBedException.getMessage());
            return buildMessage(chatId, petBedException.getErrorCode().getUserMessage());
        }

        //TODO add tests for this
        if (e instanceof ConstraintViolationException constraintEx) {
            log.warn("Validation error: {}", constraintEx.getMessage());
            return buildMessage(chatId, PetBedException.ErrorCode.VALIDATION_ERROR.getUserMessage());
        }

        log.error("Unexpected error while processing update: {}", update.getUpdateId(), e);
        return buildMessage(chatId, PetBedException.ErrorCode.INTERNAL_ERROR.getUserMessage());
    }

    private static @Nullable Long extractChatId(@Nullable Update update) {
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
        return SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
    }
}
