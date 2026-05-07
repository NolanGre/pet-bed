package op.edu.ua.petbed.telegram.callback.handler.adoption;

import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for ADOPTION callback - shows the main adoption menu.
 */
@NullMarked
@Component
public class AdoptionMenuCallbackHandler implements CallbackHandler {

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("🏠 Адопція\n\nТут ви можете віддати тварину або знайти собі нового друга.")
                .keyboard(InlineKeyboardBuilder.builder()
                        .navButtonsFor(CallbackId.ADOPTION)
                        .backButtonFor(CallbackId.ADOPTION)
                        .build())
                .build();
    }
}
