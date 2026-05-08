package op.edu.ua.petbed.telegram.callback.handler.adoption;

import lombok.RequiredArgsConstructor;
import op.edu.ua.petbed.adoption.AdoptionResponseService;
import op.edu.ua.petbed.adoption.domain.model.AdoptionResponseStatus;
import op.edu.ua.petbed.common.dto.AdoptionResponseDTO;

import op.edu.ua.petbed.telegram.callback.CallbackHandler;
import op.edu.ua.petbed.telegram.callback.CallbackId;
import op.edu.ua.petbed.telegram.callback.CallbackQueryContext;
import op.edu.ua.petbed.telegram.response.InlineKeyboardBuilder;
import op.edu.ua.petbed.telegram.response.ResponseBuilder;
import org.jspecify.annotations.NullMarked;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.botapimethods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for ADOPTION_RESPONSE_SINGLE callback - shows single response details with action buttons.
 */
@NullMarked
@Component
@RequiredArgsConstructor
public class AdoptionResponseDetailOwnerCallbackHandler implements CallbackHandler {

    private final AdoptionResponseService adoptionResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.ADOPTION_RESPONSE_SINGLE;
    }

    @Override
    public BotApiMethod<?> handle(CallbackQueryContext context) {
        Long responseId = context.callbackData().entityId();
        if (responseId == null) {
            return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                    .text("❌ Помилка: ID відгуку не вказано")
                    .keyboard(InlineKeyboardBuilder.builder()
                            .backButtonTo(CallbackId.ADOPTION_POST_DETAIL)
                            .build())
                    .build();
        }

        AdoptionResponseDTO response = adoptionResponseService.findById(responseId);

        String text = formatResponseInfo(response);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(text)
                .keyboard(buildKeyboard(response, responseId))
                .build();
    }

    private String formatResponseInfo(AdoptionResponseDTO response) {
        StringBuilder text = new StringBuilder();
        text.append("👤 @").append(response.responderUsername()).append("\n\n");
        text.append("Статус: ").append(response.getStatusEmoji()).append("\n");

        if (!response.comment().isBlank()) {
            text.append("\n💬 ").append(response.comment()).append("\n");
        }

        switch (response.status()) {
            case CONFIRMED_BY_OWNER -> text.append("\n⏳ Очікує фінального підтвердження від охочого\n");
            case FINAL_CONFIRMED -> text.append("\n✅ Передачу завершено\n");
            default -> {
            }
        }

        return text.toString();
    }

    private InlineKeyboardMarkup buildKeyboard(AdoptionResponseDTO response, Long responseId) {
        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.ADOPTION_RESPONSE_SINGLE, responseId, child -> {
                    var status = response.status();
                    if (status == AdoptionResponseStatus.NEW) {
                        return child == CallbackId.ADOPTION_RESPONSE_SINGLE_TRY_CONFIRM
                                || child == CallbackId.ADOPTION_RESPONSE_SINGLE_TRY_REJECT;
                    }
                    if (status == AdoptionResponseStatus.REJECTED_BY_OWNER) {
                        return child == CallbackId.ADOPTION_RESPONSE_SINGLE_TRY_RESTORE;
                    }
                    return false;
                })
                .backButtonTo(CallbackId.ADOPTION_RESPONSES_LIST, response.postId())
                .build();
    }
}
