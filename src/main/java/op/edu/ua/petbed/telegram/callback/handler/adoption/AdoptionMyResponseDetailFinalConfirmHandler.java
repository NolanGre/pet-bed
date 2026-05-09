package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.common.dto.AdoptionResponseDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for ADOPTION_MY_RESPONSE_DETAIL_FINAL_CONFIRM - shows confirmation dialog
 * before the RESPONDER (the user who submitted the response) does final confirmation.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionMyResponseDetailFinalConfirmHandler implements CallbackHandler {

    private final AdoptionResponseService adoptionResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_MY_RESPONSE_DETAIL_FINAL_CONFIRM;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            throw new PetBedException("Entity callback must be not null.", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        adoptionResponseService.findById(responseId);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        🏁 Фінальне підтвердження

                        Ви підтверджуєте завершення передачі тварини?

                        Після підтвердження статус зміниться на "✅ Завершено"
                        і ви вже не зможете скасувати відгук.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("✅ Так, підтвердити",
                                CallbackId.ADOPTION_MY_RESPONSE_FINAL_CONFIRM, responseId)
                        .backButtonTo(CallbackId.ADOPTION_MY_RESPONSE_DETAIL, responseId)
                        .build())
                .build();
    }
}
