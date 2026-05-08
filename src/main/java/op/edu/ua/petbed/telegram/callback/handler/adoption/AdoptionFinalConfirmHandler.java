package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
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
 * Handler for final confirmation by the responder (ADOPTION_FINAL_CONFIRM equivalent).
 * Note: Using ADOPTION_RESPONSE_DETAIL_CANCEL as the callback ID based on existing enum.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionFinalConfirmHandler implements CallbackHandler {

    private final AdoptionResponseService adoptionResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_MY_RESPONSE_DETAIL_CANCEL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            throw new PetBedException("Entity callback must be not null.", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        Long responderId = context.auth().userInternalId();

        adoptionResponseService.finalConfirm(responseId, responderId);

        return buildSuccessfulMessage(context);
    }

    private EditMessageText buildSuccessfulMessage(CallbackQueryContext context) {
        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        ✅ Передачу підтверджено!
                        
                        Вітаємо! Тварина тепер ваша.
                        Зв'яжіться з попереднім власником для організації переїзду тварини.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.ADOPTION)
                        .build())
                .build();
    }
}
