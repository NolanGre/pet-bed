package op.edu.ua.petbed.telegram.callback.handler.fostering;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import op.edu.ua.petbed.fostering.FosteringResponseService;
import op.edu.ua.petbed.common.dto.FosteringResponseDTO;
import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;

/**
 * Handler for FOSTERING_RESPONSE_DETAIL_TRY_CONFIRM - shows confirmation dialog.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringResponseDetailTryConfirmHandler implements CallbackHandler {

    private final FosteringResponseService fosteringResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_RESPONSE_DETAIL_TRY_CONFIRM;
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

        FosteringResponseDTO response = fosteringResponseService.findById(responseId);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text("""
                        ⚠️ Підтвердити відгук?

                        При підтвердженні цього відгуку:
                        - Інші відгуки будуть автоматично відхилені
                        - Охочий отримає повідомлення про ваш вибір
                        - Охочий зможе фінально підтвердити перетримку

                        Користувач: @%s
                        """.formatted(response.responderTelegramUsername()))
                .keyboard(InlineKeyboardBuilder.builder()
                        .addButton("✅ Так, підтвердити",
                                CallbackId.FOSTERING_RESPONSE_DETAIL_CONFIRM, responseId)
                        .backButtonTo(CallbackId.FOSTERING_RESPONSE_DETAIL, responseId)
                        .build())
                .build();
    }
}
