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
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;

/**
 * Handler for FOSTERING_TRY_CONFIRM callback - shows confirmation dialog before final submission.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringTryConfirmHandler implements CallbackHandler {

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_TRY_CONFIRM;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        ✅ Ви впевнені, що хочете створити оголошення про перетримку?

                        Після підтвердження оголошення буде опубліковано.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("🏁 Підтвердити", CallbackId.FOSTERING_FINAL_CONFIRM)
                        .backButtonTo(CallbackId.FOSTERING_GIVE)
                        .build())
                .build();
    }
}