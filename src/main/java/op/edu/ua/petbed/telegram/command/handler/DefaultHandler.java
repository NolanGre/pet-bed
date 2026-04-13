package op.edu.ua.petbed.telegram.command.handler;

import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@NullMarked
@Component
public class DefaultHandler implements CommandHandler {

    @Override
    public Command getCommand() {
        return Command.DEFAULT;
    }

    @Override
    public BotApiMethod<?> handle(CommandContext context) {
        return SendMessage.builder()
                .chatId(context.chatId())
                .text("Unknown command. Use /help for available commands.")
                .build();
    }
}