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

@NullMarked
@Component
@RequiredArgsConstructor
public class FosteringMyResponseDetailFinalConfirmHandler implements CallbackHandler {

    private final FosteringResponseService fosteringResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_MY_RESPONSE_DETAIL_FINAL_CONFIRM;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            throw new PetBedException("Entity callback must be not null.", PetBedException.ErrorCode.INVALID_CALLBACK);
        }

        fosteringResponseService.findById(responseId);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        🏁 Фінальне підтвердження

                        Ви підтверджуєте початок перетримки тварини?

                        Після підтвердження статус зміниться на "На перетримці"
                        і ви вже не зможете скасувати відгук.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("✅ Так, підтвердити",
                                CallbackId.FOSTERING_MY_RESPONSE_FINAL_CONFIRM, responseId)
                        .backButtonTo(CallbackId.FOSTERING_MY_RESPONSE_DETAIL, responseId)
                        .build())
                .build();
    }
}
