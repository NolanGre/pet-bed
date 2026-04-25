package op.edu.ua.petbed.telegram.command.handler;


import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.command.Command;
import op.edu.ua.petbed.telegram.command.CommandContext;
import op.edu.ua.petbed.telegram.command.CommandHandler;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@NullMarked
@Component
public class MenuHandler implements CommandHandler {

    @Override
    public Command getCommand() {
        return Command.MENU;
    }

    @Override
    public BotApiMethod<?> handle(CommandContext context) {
        return ResponseBuilder.telegram()
                .chatId(context.chatId())
                .text("🏠 Меню")
                .keyboard(InlineKeyboardBuilder.builder()
                        .navButtonsFor(CallbackId.MENU)
                        .build())
                .build();
    }
}
