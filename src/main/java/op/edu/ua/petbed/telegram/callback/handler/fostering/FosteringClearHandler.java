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
 * Handler for FOSTERING_CLEAR callback - shows confirmation dialog before clearing form.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringClearHandler implements CallbackHandler {

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_CLEAR;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        🗑️ Ви впевнені, що хочете очистити форму?

                        Усі введені дані буде втрачено.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("✅ Підтвердити", CallbackId.FOSTERING_CLEAR_CONFIRM)
                        .backButtonTo(CallbackId.FOSTERING_GIVE)
                        .build())
                .build();
    }
}