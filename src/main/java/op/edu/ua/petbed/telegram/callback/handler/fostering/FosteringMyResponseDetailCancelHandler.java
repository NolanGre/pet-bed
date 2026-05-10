package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

@NullMarked
@Component
@RequiredArgsConstructor
public class FosteringMyResponseDetailCancelHandler implements CallbackHandler {

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_MY_RESPONSE_DETAIL_CANCEL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID відгуку не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonTo(CallbackId.FOSTERING_MY_RESPONSES)
                            .build())
                    .build();
        }

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        Ви впевнені що хочете відмінити свій відгук?
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("✅ Так, відмінити",
                                CallbackId.FOSTERING_MY_RESPONSE_DETAIL_CANCEL_CONFIRM, responseId)
                        .backButtonTo(CallbackId.FOSTERING_MY_RESPONSE_DETAIL, responseId)
                        .build())
                .build();
    }
}
