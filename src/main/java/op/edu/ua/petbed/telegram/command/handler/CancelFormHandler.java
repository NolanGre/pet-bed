package op.edu.ua.petbed.telegram.command.handler;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import op.edu.ua.petbed.telegram.form.FormEntity;
import op.edu.ua.petbed.telegram.form.scheme.FormStep;
import op.edu.ua.petbed.telegram.service.FormService;
import op.edu.ua.petbed.telegram.service.TelegramMessageService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@NullMarked
@Component
@RequiredArgsConstructor
public class CancelFormHandler implements CommandHandler {

    private final FormService formService;
    private final TelegramMessageService telegramMessageService;

    @Override
    public Command getCommand() {
        return Command.CANCEL_FORM;
    }

    @Override
    public BotApiMethod<?> handle(CommandContext context) {
        Long userId = context.userAuthContext().userInternalId();
        Long chatId = context.chatId();
        
        // Get form before canceling to check if current step had keyboard
        FormEntity entity = formService.getActiveFormOrThrow(userId);
        FormStep currentStep = entity.nextStep();
        Integer lastMessageId = entity.getLastMessageId();
        
        // Remove keyboard from last message if current step had one
        if (currentStep.isChoice() && lastMessageId != null) {
            telegramMessageService.removeKeyboard(chatId, lastMessageId);
        }
        
        return formService.cancelForm(userId, chatId);
    }
}