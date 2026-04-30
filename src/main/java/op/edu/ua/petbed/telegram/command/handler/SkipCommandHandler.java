package op.edu.ua.petbed.telegram.command.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import op.edu.ua.petbed.telegram.service.FormService;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Slf4j
@NullMarked
@Component
@RequiredArgsConstructor
public class SkipCommandHandler implements CommandHandler {

    private final TelegramClient telegramClient;
    private final FormService formService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public Command getCommand() {
        return Command.SKIP_FORM_STEP;
    }

    @Override
    public BotApiMethod<?> handle(CommandContext context) {
        Long userId = context.userAuthContext().userInternalId();
        Long chatId = context.chatId();
        
        if (formService.canSkipCurrentStep(userId)) {
            // Remove keyboard from last form message if exists
            Integer lastMessageId = formService.getLastMessageId(userId);
            if (lastMessageId != null) {
                telegramMessageService.removeKeyboard(chatId, lastMessageId);
            }
            
            try {
                telegramClient.execute(ResponseBuilder.sendMessage(chatId)
                        .text("ℹ️ Крок пропущено")
                        .build());
            } catch (TelegramApiException e) {
                log.error("Failed to send skip notification", e);
            }
        }
        return formService.skipStep(userId, chatId);
    }
}
