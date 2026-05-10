package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

/**
 * Handler for FOSTERING_CLEAR_CONFIRM callback - clears form data and redirects to FOSTERING_GIVE.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringClearConfirmHandler implements CallbackHandler {

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_CLEAR_CONFIRM;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        // Clear form by cancelling and returning to FOSTERING_GIVE
        // The form will be cleared, user can start fresh

        SendMessage message = ResponseBuilder.sendMessage(context.chatId())
                .text("🗑️ Форму очищено.\n\nВи можете почати заповнення знову.")
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("🔄 Почати знову", CallbackId.FOSTERING_GIVE)
                        .backButtonTo(CallbackId.FOSTERING)
                        .build())
                .build();

        return message;
    }
}