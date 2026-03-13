package op.edu.ua.petbed.telegram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.exceptions.WebhookExceptionHandler;
import op.edu.ua.petbed.telegram.UpdateHandlerService;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpdateHandlerServiceImpl implements UpdateHandlerService {

    public BotApiMethod<?> handle(Update update) {
        try {
            validateUpdate(update);
            return handleTextMessage(update);
        } catch (Exception e) {
            return WebhookExceptionHandler.handle(update, e);
        }
    }

    private void validateUpdate(Update update) {
        if (!update.hasMessage()) {
            throw new PetBedException("Message is empty", PetBedException.ErrorCode.UNSUPPORTED_UPDATE);
        }
        if (!update.getMessage().hasText()) {
            throw new PetBedException("Updates are not supported (only text)", PetBedException.ErrorCode.UNSUPPORTED_UPDATE);
        }
    }

    private BotApiMethod<?> handleTextMessage(Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        log.info("Received message from chatId={}: {}", chatId, text);

        if (text.length() > 10) {
            throw new RuntimeException("Message is too long");
        }

        return SendMessage.builder()
                .chatId(chatId)
                .text("You wrote: " + text)
                .build();
    }
}
