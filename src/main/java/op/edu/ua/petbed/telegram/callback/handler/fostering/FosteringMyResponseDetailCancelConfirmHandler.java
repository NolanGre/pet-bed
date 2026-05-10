package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.fostering.FosteringResponseService;
import op.edu.ua.petbed.common.dto.FosteringResponseDTO;
import op.edu.ua.petbed.common.exceptions.PetBedException;
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
public class FosteringMyResponseDetailCancelConfirmHandler implements CallbackHandler {

    private final FosteringResponseService fosteringResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_MY_RESPONSE_DETAIL_CANCEL_CONFIRM;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            throw new PetBedException("ID відгуку не вказано",
                    PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        Long responderId = context.auth().userInternalId();

        FosteringResponseDTO response = fosteringResponseService.findById(responseId);

        if (!response.responderId().equals(responderId)) {
            throw new PetBedException("Ви не можете скасувати цей відгук",
                    PetBedException.ErrorCode.ADOPTION_RESPONSE_NOT_AUTHORIZED);
        }

        fosteringResponseService.declineFinalization(responseId, responderId);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("🗑️ Ваш відгук скасовано")
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonTo(CallbackId.FOSTERING_MY_RESPONSES)
                        .build())
                .build();
    }
}
