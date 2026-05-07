package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.adoption.dto.AdoptionResponseDTO;

import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for ADOPTION_RESPONSE_DETAIL_TRY_CANCEL - shows rejection confirmation dialog.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionResponseTryRejectHandler implements CallbackHandler {

    private final AdoptionResponseService adoptionResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_RESPONSE_DETAIL_TRY_CANCEL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID відгуку не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonTo(CallbackId.ADOPTION_RESPONSE_DETAIL)
                            .build())
                    .build();
        }

        AdoptionResponseDTO response = adoptionResponseService.findById(responseId);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        ⚠️ Відхилити відгук?
                        
                        Користувач @%s буде повідомлений про відхилення.
                        
                        Ви зможете відновити цей відгук пізніше, якщо передумаєте.
                        """.formatted(response.responderUsername()))
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("❌ Так, відхилити",
                                CallbackId.ADOPTION_RESPONSE_DETAIL_CANCEL, responseId)
                        .backButtonTo(CallbackId.ADOPTION_RESPONSE_DETAIL)
                        .build())
                .build();
    }
}
