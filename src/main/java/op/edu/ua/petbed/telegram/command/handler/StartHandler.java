package op.edu.ua.petbed.telegram.command.handler;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.telegram.auth.TelegramAuthService;
import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@NullMarked
@Component
@RequiredArgsConstructor
public class StartHandler implements CommandHandler {

    private final TelegramAuthService authService;

    @Override
    public Command getCommand() {
        return Command.START;
    }

    @Override
    public BotApiMethod<?> handle(CommandContext context) {
        return SendMessage.builder()
                .chatId(context.chatId())
                .text("""
                        Welcome to PetBed Bot! 🐾
                        
                        I can help you find adoptable pets and manage foster care.
                        Use /menu
                        """)
                .build();
    }
}