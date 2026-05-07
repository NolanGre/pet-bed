package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.adoption.application.dto.AdoptionResponseDTO;
import op.edu.ua.petbed.telegram.callback.CallbackData;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for ADOPTION_RESPONSE_DETAIL_TRY_CONFIRM - shows confirmation dialog.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionResponseTryConfirmHandler implements CallbackHandler {

    private final AdoptionResponseService adoptionResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_RESPONSE_DETAIL_TRY_CONFIRM;
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
                        ⚠️ Підтвердити відгук?
                        
                        При підтвердженні цього відгуку:
                        - Інші відгуки будуть автоматично відхилені
                        - Охочий отримає повідомлення про ваш вибір
                        - Охочий зможе фінально підтвердити передачу
                        
                        Користувач: @%s
                        """.formatted(response.responderUsername()))
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("✅ Так, підтвердити",
                                CallbackData.of(CallbackId.ADOPTION_RESPONSE_DETAIL_CONFIRM, responseId, null))
                        .backButtonTo(CallbackId.ADOPTION_RESPONSE_DETAIL)
                        .build())
                .build();
    }
}
