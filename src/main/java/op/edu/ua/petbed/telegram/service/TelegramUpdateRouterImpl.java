package op.edu.ua.petbed.telegram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.common.exceptions.WebhookExceptionHandler;
import op.edu.ua.petbed.telegram.TelegramUpdateRouter;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramUpdateRouterImpl implements TelegramUpdateRouter {

    // ---- Services

    public BotApiMethod<?> route(Update update) {
        try {
            if (update.hasMessage()) {
                if (isCommand(update)) {
                    return handleCommand(update);
                }
                return handleTextMessage(update);
            }
            if (update.hasCallbackQuery()) {
                return handleCallback(update);
            }
            throw new PetBedException("Unsupported update type", PetBedException.ErrorCode.UNSUPPORTED_UPDATE);
        } catch (Exception e) {
            return WebhookExceptionHandler.handle(update, e);
        }
    }

    private boolean isCommand(Update update) {
        List<MessageEntity> entities = update.getMessage().getEntities();
        return entities != null && entities.stream()
                .anyMatch(e -> "bot_command".equals(e.getType()));
    }

    private BotApiMethod<?> handleCommand(Update update) {
        return null;
    }

    private BotApiMethod<?> handleTextMessage(Update update) {
        return null;
    }

    private BotApiMethod<?> handleCallback(Update update) {
        return null;
    }
}
