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
        // This should be ADOPTION_FINAL_CONFIRM but it's not in the enum
        // Using ADOPTION_MY_RESPONSE_DETAIL_CANCEL as placeholder
        return CallbackId.ADOPTION_MY_RESPONSE_DETAIL_CANCEL;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID відгуку не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonFor(CallbackId.ADOPTION_MY_RESPONSES)
                            .build())
                    .build();
        }

        Long responderId = context.auth().userInternalId();

        adoptionResponseService.finalConfirm(responseId, responderId);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        ✅ Передачу підтверджено!
                        
                        Вітаємо! Тварина тепер ваша.
                        Зв'яжіться з попереднім власником для організації переїзду тварини.
                        """)
                .keyboard(InlineKeyboardBuilder.builder()
                        .backButtonFor(CallbackId.ADOPTION)
                        .build())
                .build();
    }
}
