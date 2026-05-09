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
 * Handler for ADOPTION_MY_RESPONSE_DETAIL_CANCEL_CONFIRM - executes response cancellation.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionMyResponseDetailCancelConfirmHandler implements CallbackHandler {

    private final AdoptionResponseService adoptionResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_MY_RESPONSE_DETAIL_CANCEL_CONFIRM;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            throw new PetBedException("ID відгуку не вказано",
                    PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        Long responderId = context.auth().userInternalId();

        AdoptionResponseDTO response = adoptionResponseService.findById(responseId);

if (!response.responderId().equals(responderId)) {
            throw new PetBedException("Ви не можете скасувати цей відгук",
                    PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED);
        }

        adoptionResponseService.declineFinalization(responseId, responderId);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("🗑️ Ваш відгук скасовано")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.ADOPTION_MY_RESPONSES)
                        .build())
                .build();
    }
}