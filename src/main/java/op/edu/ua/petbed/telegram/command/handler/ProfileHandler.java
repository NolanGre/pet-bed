package op.edu.ua.petbed.telegram.command.handler;

import op.edu.ua.petbed.telegram.auth.TelegramAuthService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

@NullMarked
@Component
public class ProfileHandler implements CommandHandler {

    private final TelegramAuthService authService;

    public ProfileHandler(TelegramAuthService authService) {
        this.authService = authService;
    }

    @Override
    public Command getCommand() {
        return Command.PROFILE;
    }

    @Override
    public BotApiMethod<?> handle(CommandContext context) {
        UserAuthContext auth = authService.authenticate(context.userId(), context.username());
        
        return SendMessage.builder()
                .chatId(context.chatId())
                .text(String.format("""
                    👤 Your Profile
                    
                    ID: %d
                    Username: %s
                    Type: %s
                    """,
                        auth.userId(),
                        context.username() != null ? context.username() : "N/A",
                        auth.userType()))
                .build();
    }
}