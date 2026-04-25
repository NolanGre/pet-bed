package op.edu.ua.petbed.telegram.command.handler;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.telegram.auth.TelegramAuthService;
import op.edu.ua.petbed.telegram.auth.UserAuthContext;
import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@NullMarked
@Component
@RequiredArgsConstructor
public class ProfileHandler implements CommandHandler {

    private final TelegramAuthService authService;

    @Override
    public Command getCommand() {
        return Command.PROFILE;
    }

    @Override
    public BotApiMethod<?> handle(CommandContext context) {
        var auth = context.userAuthContext();
        return ResponseBuilder.sendMessage(context.chatId())
                .text(String.format("""
                    👤 Your Profile
                    
                    ID: %d
                    Username: %s
                    Type: %s
                    """,
                        auth.userInternalId(),
                        auth.username(),
                        auth.userType()))
                .build();
    }
}