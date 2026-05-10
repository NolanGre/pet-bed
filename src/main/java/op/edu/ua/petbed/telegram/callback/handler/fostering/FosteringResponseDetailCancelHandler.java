package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.fostering.FosteringResponseService;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for FOSTERING_RESPONSE_DETAIL_CANCEL - actually rejects the response.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringResponseDetailCancelHandler implements CallbackHandler {

    private final FosteringResponseService fosteringResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_RESPONSE_DETAIL_CANCEL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID відгуку не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonFor(CallbackId.FOSTERING_MY_POSTS)
                            .build())
                    .build();
        }

        Long ownerId = context.auth().userInternalId();

        fosteringResponseService.rejectByOwner(responseId, ownerId);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        ❌ Відгук відхилено.

                        Користувач отримає повідомлення.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.FOSTERING_RESPONSE_DETAIL)
                        .build())
                .build();
    }
}
