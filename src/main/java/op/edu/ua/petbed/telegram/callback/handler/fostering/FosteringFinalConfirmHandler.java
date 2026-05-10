package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.fostering.FosteringResponseService;
import op.edu.ua.petbed.common.exceptions.PetBedException;
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
 * Handler for FOSTERING_MY_RESPONSE_FINAL_CONFIRM callback.
 * Executes final confirmation by the responder to complete the fostering.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class FosteringFinalConfirmHandler implements CallbackHandler {

    private final FosteringResponseService fosteringResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_MY_RESPONSE_FINAL_CONFIRM;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            throw new PetBedException("Entity callback must be not null.", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        Long responderId = context.auth().userInternalId();

        fosteringResponseService.finalConfirm(responseId, responderId);

        return buildSuccessfulMessage(context);
    }

    private EditMessageText buildSuccessfulMessage(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        ✅ Перетримку підтверджено!

                        Тварина тепер на перетримці у вас.
                        Зв'яжіться з власником для отримання тварини.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.FOSTERING)
                        .build())
                .build();
    }
}