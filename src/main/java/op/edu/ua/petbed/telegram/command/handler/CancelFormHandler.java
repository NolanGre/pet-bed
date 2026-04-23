package op.edu.ua.petbed.telegram.command.handler;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import op.edu.ua.petbed.telegram.service.FormService;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@NullMarked
@Component
@RequiredArgsConstructor
public class CancelFormHandler implements CommandHandler {

    private final FormService formService;

    @Override
    public Command getCommand() {
        return Command.CANCEL_FORM;
    }

    @Override
    public BotApiMethod<?> handle(CommandContext context) {
        return formService.cancelForm(context.userId());
    }
}
