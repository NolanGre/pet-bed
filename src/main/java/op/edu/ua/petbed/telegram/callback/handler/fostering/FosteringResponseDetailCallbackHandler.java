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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;

/**
 * Handler for FOSTERING_RESPONSE_DETAIL callback - shows single response details with action buttons.
 */
@NullMarked
@Component
@RequiredArgsConstructor
@Slf4j
public class FosteringResponseDetailCallbackHandler implements CallbackHandler {

    private final FosteringResponseService fosteringResponseService;

    @Override
    public CallbackId getCallbackId() {
        return CallbackId.FOSTERING_RESPONSE_DETAIL;
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

        String text = formatResponseInfo(response);

        return ResponseBuilder.editMessage(context.chatId(), context.messageId())
                .text(text)
                .keyboard(buildKeyboard(response, responseId))
                .build();
    }

    private String formatResponseInfo(FosteringResponseDTO response) {
        StringBuilder text = new StringBuilder();
        String username = response.responderTelegramUsername();
        if (username != null && !username.isBlank()) {
            text.append("👤 @").append(username).append("\n\n");
        } else {
            text.append("👤 Користувач\n\n");
        }
        text.append("Статус: ").append(response.getStatusEmoji()).append("\n");

        if (response.comment() != null && !response.comment().isBlank()) {
            text.append("\n💬 ").append(response.comment()).append("\n");
        }

        switch (response.status()) {
            case CONFIRMED_BY_OWNER -> text.append("\n⏳ Очікує фінального підтвердження від охочого\n");
            case FINAL_CONFIRMED -> text.append("\n✅ Перетримку підтверджено\n");
            default -> {
            }
        }

        return text.toString();
    }

    private InlineKeyboardMarkup buildKeyboard(FosteringResponseDTO response, Long responseId) {
        return InlineKeyboardBuilder.builder()
                .navButtonsFor(CallbackId.FOSTERING_RESPONSE_DETAIL, responseId, child -> {
                    var status = response.status();
                    if (status == op.edu.ua.petbed.fostering.domain.model.FosteringResponseStatus.NEW) {
                        return child == CallbackId.FOSTERING_RESPONSE_DETAIL_TRY_CONFIRM
                                || child == CallbackId.FOSTERING_RESPONSE_DETAIL_TRY_CANCEL;
                    }
                    return false;
                })
                .backButtonTo(CallbackId.FOSTERING_POST_DETAIL, response.postId())
                .build();
    }
}
