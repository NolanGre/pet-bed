package op.edu.ua.petbed.telegram.callback.handler;

import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@Component
@NullMarked
public class MenuCallbackHandler implements CallbackHandler {

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.MENU;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        return ResponseBuilder.telegram()
                .chatId(context.chatId())
                .text("Меню")
                .keyboard(InlineKeyboardBuilder.builder()
                        .navButtonsFor(CallbackId.MENU)
                        .build())
                .editMessage(context.messageId())
                .build();
    }
}
