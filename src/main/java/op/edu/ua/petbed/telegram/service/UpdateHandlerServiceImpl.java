package op.edu.ua.petbed.telegram.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        if (update.hasMessage() && update.getMessage().hasText()) {   //TODO: images, locations
            return handleTextMessage(update);
        }

        log.warn("Received unsupported update type: {}", update.getUpdateId());
        return null;
    }

    private BotApiMethod<?> handleTextMessage(Update update) {
        String text = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        log.info("Received message from chatId={}: {}", chatId, text);

        return SendMessage.builder()
                .chatId(chatId)
                .text("You wrote: " + text)
                .build();
    }
}
